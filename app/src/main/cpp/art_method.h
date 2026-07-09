/*
 * Copyright AngelToms
 * SPDX-License-Identifier: Apache-2.0
 */

/**
 * @Atuthor : AngelToms
 * @Version: 0.2
 * @Desc: art hook
 * @Theory: clone method, jave proxy, reflect
 * @Date: 2024
 * 实现方式其实有很多种，这种或许最简单
 * 另外检测是哪个版本也有很多种方式，如检测系统属性，检测apilevel
 * @Additional
 * 上层ArtMethod的native实现
 * 我去掉了其他让人模糊的地方，这样代码看起来更简单，并且该方式仅在5.0 6.0手机上测试过
 * 对于更高版你需要定制（按照这里的思路），或者它根本不支持
 * 我本人之测过几部手机，对于兼容性不敢保证，此实现仅用于学习和讨论
 * 如果用于产品由于兼容性问题请自发解决（因为原理很简单）
 * 注意：
 * 实际上hook不能简单的改变或者复制ArtMethod就能达到目的，特别是在call back的场景
 * ArtMethod不是一个孤立的结构体，别的结构关联着它，因此简单的复制会导致问题
 * 时间有限，我没深入call origin，此仅作为demo
 * 产品级别的需求，还是参考其他优良的框架吧
 * 网上有两种主流不同的实现，Trampoline和Slot Placement，灵感来源全是源码，对，记得深入研究源码!!!
 *
 */

#ifndef ART_METHOD_H_
#define ART_METHOD_H_

#include <jni.h>
#include <string.h>
#include <stdio.h>

#include "type.h"

#include<android/log.h>

#define ALOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "HOOK_REPACKER", __VA_ARGS__))
#define ALOGE(...) ((void)__android_log_print(ANDROID_LOG_ERROR, "HOOK_REPACKER", __VA_ARGS__))

#define PACKED(x) __attribute__ ((__aligned__(x), __packed__))

#define kAccCompileDonated       0x04000000U
#define kAccInterpreterBridge    0x10000000U
#define kAccSkipAccessChecks     0x00010000U

#ifdef __cplusplus
extern "C" {
#endif

typedef enum  {
    type_android_unkwon = 0, type_android4x, type_androidl, type_android5, type_android6, type_android7, type_android8
} Android_Type;

// 为测试art method版本设计
typedef struct {
u4 padding_[20];
} ArtMethod __attribute__((aligned(4)));

/**
 * 注意：
 * ArtMethod结构在同一个Android版本也存在不同的结构
 * 我这里只是列举了我手机版本对应的相同结构
 * 如果你想在你的手机上测试，你需要修改ArtMethod结构
 */
typedef struct {
    u4 klass_;
    u4 monitor_;
    void* declaring_class_;
    void* dex_cache_initialized_static_storage_;
    void* dex_cache_resolved_methods_;
    void* dex_cache_resolved_types_;
    void* dex_cache_strings_;
    u4 access_flags_;
    u4 code_item_offset_;
    u4 core_spill_mask_;
    const void* entry_point_from_compiled_code_;
    void* entry_point_from_interpreter_;
    u4 fp_spill_mask_;
    size_t frame_size_in_bytes_;
    const u2* gc_map_;
    const u4* mapping_table_;
    u4 method_dex_index_;
    u4 method_index_;
    const void* native_method_;
    const u2* vmap_table_;
} ArtMethod4x;

typedef struct {
    u4 klass_;
    u4 monitor_;
    u4 declaring_class_;
    u4 dex_cache_resolved_methods_;
    u4 dex_cache_resolved_types_;
    u4 dex_cache_strings_;
    u8 entry_point_from_interpreter_;
    u8 entry_point_from_jni_;
    u8 entry_point_from_portable_compiled_code_;
    u8 entry_point_from_quick_compiled_code_;
    u8 gc_map_;
    u4 access_flags_;
    u4 dex_code_item_offset_;
    u4 dex_method_index_;
    u4 method_index_;
} ArtMethodl;

typedef struct  {
    u4 klass_;
    u4 monitor_;
    u4 declaring_class_;
    u4 dex_cache_resolved_methods_;
    u4 dex_cache_resolved_types_;
    u4 dex_cache_strings_;
    u8 entry_point_from_interpreter_;
    u8 entry_point_from_jni_;
    u8 entry_point_from_quick_compiled_code_;
    u8 gc_map_;
    u4 access_flags_;
    u4 dex_code_item_offset_;
    u4 dex_method_index_;
    u4 method_index_;
} ArtMethod5; // android-5.0.0_rxxx

typedef struct  {
    u4 klass_;
    u4 monitor_;
    u4 declaring_class_;
    u4 dex_cache_resolved_methods_;
    u4 dex_cache_resolved_types_;
    u4 access_flags_;
    u4 dex_code_item_offset_;
    u4 dex_method_index_;
    u4 method_index_;
    struct PACKED(4) {
        void* entry_point_from_interpreter_;
        void* entry_point_from_jni_;
        void* entry_point_from_quick_compiled_code_;
    };
} ArtMethod5_1; // android-5.1.x_rxxx

typedef struct /*PACKED(4)*/ {
    u4 declaring_class_;
    u4 dex_cache_resolved_methods_;
    u4 dex_cache_resolved_types_;
    u4 access_flags_;
    u4 dex_code_item_offset_;
    u4 dex_method_index_;
    u4 method_index_;
    struct PACKED(4) {
        void* entry_point_from_interpreter_;
        void* entry_point_from_jni_;
        void* entry_point_from_quick_compiled_code_;
    };
} ArtMethod6;

typedef struct {
    u4 declaring_class_;
    u4 access_flags_;
    u4 dex_code_item_offset_;
    u4 dex_method_index_;
    u2 method_index_;
    u2 hotness_count_;
    struct PACKED(4) {
        void** dex_cache_resolved_methods_;
        void* dex_cache_resolved_types_;
        void* entry_point_from_jni_;
        void* entry_point_from_quick_compiled_code_;
    };
} ArtMethod7;

typedef struct {
    u4 declaring_class_;
    u4 access_flags_;
    u4 dex_code_item_offset_;
    u4 dex_method_index_;
    u2 method_index_;
    u2 hotness_count_;
    void** dex_cache_resolved_methods_;
    void* data_; // (存放 JNI 数组指针、Profiling 或者是其所属的类/方法信息，32位4字节/64位8字节)
    void* entry_point_from_quick_compiled_code_;
} ArtMethod8; // android-8.0.x_rxx

typedef struct {
    u4 declaring_class_;
    u4 access_flags_;
    u4 dex_code_item_offset_;
    u4 dex_method_index_;
    u2 method_index_;
    u2 hotness_count_;
    void* dex_cache_resolved_methods_;
    void* data_; // (存放 JNI 数组指针、Profiling 或者是其所属的类/方法信息，32位4字节/64位8字节)
    void* entry_point_from_quick_compiled_code_;
} ArtMethod8_1; // android-8.1.x_rxx

typedef struct {
    Android_Type android_type;
    int sdk_version;
    int size;
} Method_Info;

static Method_Info minfo;

///////////////////////////////////////////////////////////////////
// 绕不过虚拟机校验
// 本想在往深层走走， 复杂， 时间有限，以后再说
// 千人千面，市面已经有很多方案，以后要是弄，就弄一个稍微轻量的，简单的
///////////////////////////////////////////////////////////////////
typedef struct {
// Defining class loader, or null for the "bootstrap" system loader.
uint32_t class_loader_;

// For array classes, the component class object for instanceof/checkcast
// (for String[][][], this will be String[][]). null for non-array classes.
    uint32_t component_type_;

// DexCache of resolved constant pool entries (will be null for classes generated by the
// runtime such as arrays and primitive classes).
    uint32_t dex_cache_;

// Short cuts to dex_cache_ member for fast compiled code access.
    uint32_t dex_cache_strings_;

// The interface table (iftable_) contains pairs of a interface class and an array of the
// interface methods. There is one pair per interface supported by this class.  That means one
// pair for each interface we support directly, indirectly via superclass, or indirectly via a
// superinterface.  This will be null if neither we nor our superclass implement any interfaces.
//
// Why we need this: given "class Foo implements Face", declare "Face faceObj = new Foo()".
// Invoke faceObj.blah(), where "blah" is part of the Face interface.  We can't easily use a
// single vtable.
//
// For every interface a concrete class implements, we create an array of the concrete vtable_
// methods for the methods in the interface.
    uint32_t iftable_;

// Descriptor for the class such as "java.lang.Class" or "[C". Lazily initialized by ComputeName
    uint32_t name_;

// The superclass, or null if this is java.lang.Object, an interface or primitive type.
    uint32_t super_class_;

// If class verify fails, we must return same error on subsequent tries.
    uint32_t verify_error_class_;

// Virtual method table (vtable), for use by "invoke-virtual".  The vtable from the superclass is
// copied in, and virtual methods from our class either replace those from the super or are
// appended. For abstract classes, methods may be created in the vtable that aren't in
// virtual_ methods_ for miranda methods.
    uint32_t vtable_;

// Access flags; low 16 bits are defined by VM spec.
// Note: Shuffled back.
uint32_t access_flags_;

// static, private, and <init> methods. Pointer to an ArtMethod array.
uint64_t direct_methods_;

// instance fields
//
// These describe the layout of the contents of an Object.
// Note that only the fields directly declared by this class are
// listed in ifields; fields declared by a superclass are listed in
// the superclass's Class.ifields.
//
// ArtField arrays are allocated as an array of fields, and not an array of fields pointers.
uint64_t ifields_;

// Static fields
uint64_t sfields_;

// Virtual methods defined in this class; invoked through vtable. Pointer to an ArtMethod array.
uint64_t virtual_methods_;

// Total size of the Class instance; used when allocating storage on gc heap.
// See also object_size_.
uint32_t class_size_;

// Tid used to check for recursive <clinit> invocation.
pid_t clinit_thread_id_;

// ClassDef index in dex file, -1 if no class definition such as an array.
// TODO: really 16bits
int32_t dex_class_def_idx_;

// Type index in dex file.
// TODO: really 16bits
int32_t dex_type_idx_;

// Number of direct fields.
uint32_t num_direct_methods_;

// Number of instance fields.
uint32_t num_instance_fields_;

// Number of instance fields that are object refs.
uint32_t num_reference_instance_fields_;

// Number of static fields that are object refs,
uint32_t num_reference_static_fields_;

// Number of static fields.
uint32_t num_static_fields_;

// Number of virtual methods.
uint32_t num_virtual_methods_;

// Total object size; used when allocating storage on gc heap.
// (For interfaces and abstract classes this will be zero.)
// See also class_size_.
uint32_t object_size_;

// The lower 16 bits contains a Primitive::Type value. The upper 16
// bits contains the size shift of the primitive type.
uint32_t primitive_type_;

// Bitmap of offsets of ifields.
uint32_t reference_instance_offsets_;

// State of class initialization.
uint32_t status_;
} MirrorClass6;

#ifdef __cplusplus
};
#endif

#endif /* ART_METHOD_H_ */