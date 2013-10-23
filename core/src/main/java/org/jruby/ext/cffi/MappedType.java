/*
 * 
 */

package org.jruby.ext.cffi;


import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.runtime.callsite.FunctionalCachingCallSite;

/**
 * A type which represents a conversion to/from a native type.
 */
@JRubyClass(name="FFI::Type::Mapped", parent="FFI::Type")
public final class MappedType extends Type {
    private final Type realType;
    private final IRubyObject converter;
    private final boolean isReferenceRequired;
    private final CachingCallSite toNativeCallSite = new FunctionalCachingCallSite("to_native");
    private final CachingCallSite fromNativeCallSite = new FunctionalCachingCallSite("from_native");

    public static RubyClass createConverterTypeClass(Ruby runtime, RubyModule module) {
        RubyClass convClass = module.getClass("Type").defineClassUnder("Mapped", module.getClass("Type"),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        convClass.defineAnnotatedMethods(MappedType.class);
        convClass.defineAnnotatedConstants(MappedType.class);


        return convClass;
    }

    private MappedType(Ruby runtime, RubyClass klass, Type nativeType, IRubyObject converter, boolean isRefererenceRequired) {
        super(runtime, klass, NativeType.MAPPED, nativeType.size(), nativeType.alignment());
        this.realType = nativeType;
        this.converter = converter;
        this.isReferenceRequired = isRefererenceRequired;
    }

    @JRubyMethod(name = "new", meta = true)
    public static final IRubyObject newMappedType(ThreadContext context, IRubyObject klass, IRubyObject converter) {
        if (!converter.respondsTo("native_type")) {
            throw context.runtime.newNoMethodError("converter needs a native_type method", "native_type", converter.getMetaClass());
        }

        Type realType;
        try {
            realType = (Type) converter.callMethod(context, "native_type");
        } catch (ClassCastException ex) {
            throw context.runtime.newTypeError("native_type did not return instance of FFI::Type");
        }

        boolean isReferenceRequired;
        if (converter.respondsTo("reference_required?")) {
            isReferenceRequired = converter.callMethod(context, "reference_required?").isTrue();

        } else {
            switch (realType.nativeType()) {
                case BOOL:
                case SCHAR:
                case UCHAR:
                case SSHORT:
                case USHORT:
                case SINT:
                case UINT:
                case SLONG:
                case ULONG:
                case SLONG_LONG:
                case ULONG_LONG:
                case FLOAT:
                case DOUBLE:
                    isReferenceRequired = false;
                    break;

                default:
                    isReferenceRequired = true;
                    break;
            }
        }

        return new MappedType(context.runtime, (RubyClass) klass, realType, converter, isReferenceRequired);
    }
    
    final Type getRealType() {
        return realType;
    }

    final boolean isReferenceRequired() {
        return isReferenceRequired;
    }

    final boolean isPostInvokeRequired() {
        return false;
    }

    @JRubyMethod
    public final IRubyObject native_type(ThreadContext context) {
        return realType;
    }

    @JRubyMethod
    public final IRubyObject from_native(ThreadContext context, IRubyObject value, IRubyObject ctx) {
        return fromNative(context, value);
    }

    @JRubyMethod
    public final IRubyObject to_native(ThreadContext context, IRubyObject value, IRubyObject ctx) {
        return toNative(context, value);
    }

    public final IRubyObject fromNative(ThreadContext context, IRubyObject value) {
        return fromNativeCallSite.call(context, this, converter, value, context.runtime.getNil());
    }

    public final IRubyObject toNative(ThreadContext context, IRubyObject value) {
        return toNativeCallSite.call(context, this, converter, value, context.runtime.getNil());
    }
}
