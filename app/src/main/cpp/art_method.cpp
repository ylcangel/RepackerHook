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
 *
 */

#include "art_method.h"

#include <jni.h>
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/mman.h>
#include <unistd.h>

extern "C" void flush_cpu_cache(void* addr) {
    size_t page_size = sysconf(_SC_PAGESIZE);
    uintptr_t page_start = reinterpret_cast<uintptr_t>(addr) & ~(page_size - 1);
    char* begin = (char*)page_start;
    char* end = begin + page_size;
    // 刷新该内存区域的 I-Cache 和 D-Cache
    __builtin___clear_cache(begin, end);
}

extern "C"
JNIEXPORT void JNICALL
 Java_com_angeltoms_repacker_hook_ArtMethod_setApiLevel(JNIEnv* env, jobject cl, int apil) {
    minfo.sdk_version = apil;
    if(apil > 27) {// unsupport up 9.0
        minfo.android_type = type_android_unkwon;
    } else if (apil > 26) {
        minfo.android_type = type_android8;
        minfo.size = sizeof(ArtMethod8);
    } else if (apil > 23) {
        minfo.android_type = type_android7;
        minfo.size = sizeof(ArtMethod7);
    } else if (apil > 22) {
        minfo.android_type = type_android6;
        minfo.size = sizeof(ArtMethod6);
    } else if (apil > 20) {
        minfo.android_type = type_android5;
        minfo.size = sizeof(ArtMethod5);
    } else if (apil > 19) {
        minfo.android_type = type_androidl;
        minfo.size = sizeof(ArtMethodl);
    } else {
        minfo.android_type = type_android4x;
        minfo.size = sizeof(ArtMethod4x);
    }

    ALOGI("Android type = %d, size = %d", minfo.android_type, minfo.size);
}

// 辅助函数：解除内存页的只读保护，允许修改 ArtMethod 结构体
static void unprotect_memory(void* addr) {
    // 从 Android 11（API 级别 30，Android R） 开始，系统对 ArtMethod 所在的内存区域实施了更为严格的只读保护（Read-Only Protection）
    if (minfo.sdk_version >= 30) { // android11
        size_t page_size = sysconf(_SC_PAGESIZE);
        // 强制计算出内存页的起始对齐地址
        uintptr_t page_start = reinterpret_cast<uintptr_t>(addr) & ~(page_size - 1);
        mprotect((void*)page_start, page_size, PROT_READ | PROT_WRITE | PROT_EXEC);
    }
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_angeltoms_repacker_hook_ArtMethod_cloneMethod(JNIEnv* env, jobject thiz, jobject src, jobject dst) {
    ALOGI("Clone. art method (Hooking...)");

    jboolean r = false;
    void* dest_method = NULL;
    void* src_method = NULL;
    jmethodID origin = env->FromReflectedMethod(src);
    jmethodID dest = env->FromReflectedMethod(dst);

    if (!origin || !dest) {
        ALOGE("Clone. find class method failed");
        goto exit;
    }

    unprotect_memory(origin);

    if(minfo.android_type == type_android5) {
        ArtMethod5* art_origin = (ArtMethod5*)origin;
        ArtMethod5* art_dest = (ArtMethod5*)dest;

        art_origin->declaring_class_ = art_dest->declaring_class_;
        art_origin->dex_code_item_offset_ = art_dest->dex_code_item_offset_;
        art_origin->entry_point_from_interpreter_ = art_dest->entry_point_from_interpreter_;
        art_origin->entry_point_from_jni_ = art_dest->entry_point_from_jni_;
        art_origin->entry_point_from_quick_compiled_code_ = art_dest->entry_point_from_quick_compiled_code_;
        art_origin->dex_cache_resolved_methods_ = art_dest->dex_cache_resolved_methods_;

    } else if(minfo.android_type == type_android6) {
        ArtMethod6* art_origin = (ArtMethod6*)origin;
        ArtMethod6* art_dest = (ArtMethod6*)dest;

        art_origin->declaring_class_ = art_dest->declaring_class_;
        art_origin->dex_code_item_offset_ = art_dest->dex_code_item_offset_;
        art_origin->entry_point_from_interpreter_ = art_dest->entry_point_from_interpreter_;
        art_origin->entry_point_from_quick_compiled_code_ = art_dest->entry_point_from_quick_compiled_code_;
        art_origin->entry_point_from_jni_ = art_dest->entry_point_from_jni_;

    } else if(minfo.android_type == type_android7) {
        ArtMethod7* art_origin = (ArtMethod7*)origin;
        ArtMethod7* art_dest = (ArtMethod7*)dest;

        art_origin->declaring_class_ = art_dest->declaring_class_;
        art_origin->hotness_count_ = 0;
        art_origin->dex_code_item_offset_ = art_dest->dex_code_item_offset_;
        art_origin->entry_point_from_quick_compiled_code_ = art_dest->entry_point_from_quick_compiled_code_;

        // 1. 先同步基础状态
        uint32_t flags = art_dest->access_flags_;
        // 强制清除 Android 7 运行时动态编译与桥接标记
        // 0x04000000: kAccCompileDonated
        // 0x10000000: kAccInterpreterBridge / kAccSingleImplementation (视具体系统小版本而定)
        flags &= ~(0x04000000U | 0x10000000U);
        // 建议加上跳过权限检查标记，防止因为变成内部调用而触发不必要的 AccessCheck 崩溃
        flags |= 0x00010000U; // kAccSkipAccessChecks
        art_origin->access_flags_ = flags;
    } else if(minfo.android_type == type_android8) {
        ArtMethod8* art_origin = (ArtMethod8*)origin;
        ArtMethod8* art_dest = (ArtMethod8*)dest;

        art_origin->declaring_class_ = art_dest->declaring_class_;
        art_origin->hotness_count_ = 0;
        art_origin->dex_code_item_offset_ = art_dest->dex_code_item_offset_;
        art_origin->entry_point_from_quick_compiled_code_ = art_dest->entry_point_from_quick_compiled_code_;

        uint32_t flags = art_dest->access_flags_;
        // 强制清除 Android 8 编译优化标记
        flags &= ~(0x04000000U | 0x10000000U);
        // 3. 加上跳过检查标记
        flags |= 0x00010000U; // kAccSkipAccessChecks
        art_origin->access_flags_ = flags;
    }

    r = true;
    ALOGI("Clone. art method (Hooking end)");

    exit:
        if(env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
    return r;
}

extern "C"
JNIEXPORT jlong JNICALL
Java_com_angeltoms_repacker_hook_ArtMethod_cloneMethod1(JNIEnv* env, jobject thiz, jobject src) {
    ALOGI("Back. clone art method ....");
    jlong r = 0;
    void* back_method = nullptr;
    void* origin_method = nullptr;

    jlong method_ptr = 0;
    jmethodID origin = env->FromReflectedMethod(src);

    if (!origin) {
        ALOGE("Back. find class method failed");
        goto exit;
    }

    if(minfo.android_type == type_android5) {
        back_method = (ArtMethod5*) malloc (minfo.size);
    } else if(minfo.android_type == type_android6) {
        back_method = (ArtMethod6*) malloc (minfo.size);
    } else if(minfo.android_type == type_android7) {
        back_method = (ArtMethod7*) malloc (minfo.size);
    } else if(minfo.android_type == type_android8) {
        back_method = (ArtMethod8*) malloc (minfo.size);
    }

    ALOGI("Back. sdk level = %d, android type = %d", minfo.sdk_version, minfo.android_type);
    if (!back_method) {
        ALOGE("Back. creat new method fail, malloc fail");
        goto exit;
    }

    origin_method = reinterpret_cast<void*>(origin);
    ALOGI("Back. origin art method ptr: %p", origin_method);
    if (minfo.android_type != type_android_unkwon && minfo.size > 0) {
        memcpy(back_method, origin_method, minfo.size);
    }

    if(minfo.android_type == type_android5) {

    } else if(minfo.android_type == type_android6) {

    } else if(minfo.android_type == type_android7) {

    } else if(minfo.android_type == type_android8) {
    }

    method_ptr = reinterpret_cast<jlong>(back_method);
    ALOGI("Back. art method ptr: %p, %llx",  back_method, method_ptr);
    ALOGI("Back. clone art method end");
    r = method_ptr;

    exit:
        if(env->ExceptionCheck()) {
            env->ExceptionDescribe();
            env->ExceptionClear();
        }
    return r;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_angeltoms_repacker_hook_ArtMethod_getBackMethod(JNIEnv *env, jobject thiz, jlong methodPtr,
                                                        jclass clazz, jboolean isStatic) {
    ALOGI("GetBack. art method ...");
    void* method = (void*) methodPtr;
    ALOGI("GetBack. art method ptr: %p", method);
    jmethodID methodId = reinterpret_cast<jmethodID>(methodPtr);
    jobject javaMethod = env->ToReflectedMethod(clazz, methodId, isStatic);

    if(env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }

    ALOGI("GetBack. art method end");
    return javaMethod;
}

extern "C"
JNIEXPORT jboolean JNICALL
Java_com_angeltoms_repacker_hook_ArtMethod_restoryMethod(JNIEnv* env, jobject thiz,
                                                         jobject src, jlong methodPtr, jboolean isShort) {
    ALOGI("Restory. art method ...");
    jboolean r = false;
    void* dest = (void*) methodPtr;
    jmethodID origin = env->FromReflectedMethod(src);

    ALOGI("Restory. origin method = %p, back method = %p", origin, dest);

    if (!origin) {
        ALOGE("Restory. find class method failed");
        goto exit;
    }

    unprotect_memory(origin);

    if(minfo.android_type == type_android5) {
        ArtMethod5* art_origin = (ArtMethod5*)origin;
        ArtMethod5* art_dest = (ArtMethod5*)dest;

        art_origin->declaring_class_ = art_dest->declaring_class_;
        art_origin->dex_code_item_offset_ = art_dest->dex_code_item_offset_;
        art_origin->entry_point_from_interpreter_ = art_dest->entry_point_from_interpreter_;
        art_origin->entry_point_from_jni_ = art_dest->entry_point_from_jni_;
        art_origin->entry_point_from_quick_compiled_code_ = art_dest->entry_point_from_quick_compiled_code_;
        art_origin->dex_cache_resolved_methods_ = art_dest->dex_cache_resolved_methods_;

    } else if(minfo.android_type == type_android6) {
        ArtMethod6* art_origin = (ArtMethod6*)origin;
        ArtMethod6* art_dest = (ArtMethod6*)dest;

        art_origin->declaring_class_ = art_dest->declaring_class_;
        art_origin->entry_point_from_interpreter_ = art_dest->entry_point_from_interpreter_;
        art_origin->entry_point_from_quick_compiled_code_ = art_dest->entry_point_from_quick_compiled_code_;
        art_origin->dex_code_item_offset_ = art_dest->dex_code_item_offset_;
        art_origin->entry_point_from_jni_ = art_dest->entry_point_from_jni_;

    } else if(minfo.android_type == type_android7) {
        ArtMethod7* art_origin = (ArtMethod7*)origin;
        ArtMethod7* art_dest = (ArtMethod7*)dest;

        art_origin->declaring_class_ = art_dest->declaring_class_;
        art_origin->hotness_count_ = art_dest->hotness_count_;
        art_origin->entry_point_from_quick_compiled_code_ = art_dest->entry_point_from_quick_compiled_code_;
        art_origin->dex_code_item_offset_ = art_dest->dex_code_item_offset_;
        art_origin->access_flags_ = art_dest->access_flags_;
    } else if(minfo.android_type == type_android8) {
        ArtMethod8* art_origin = (ArtMethod8*)origin;
        ArtMethod8* art_dest = (ArtMethod8*)dest;

        art_origin->declaring_class_ = art_dest->declaring_class_;
        art_origin->hotness_count_ = art_dest->hotness_count_;
        art_origin->entry_point_from_quick_compiled_code_ = art_dest->entry_point_from_quick_compiled_code_;
        art_origin->dex_code_item_offset_ = art_dest->dex_code_item_offset_;
        art_origin->access_flags_ = art_dest->access_flags_;
    }

    // 判断是否释放内存中保留的这个结构
    if (!isShort) {
        free(dest);
        dest = nullptr;
    }

    flush_cpu_cache(origin);

    r = true;
    ALOGI("Restory. art method end");

    exit:
    if(env->ExceptionCheck()) {
        env->ExceptionDescribe();
        env->ExceptionClear();
    }
    return r;
}
