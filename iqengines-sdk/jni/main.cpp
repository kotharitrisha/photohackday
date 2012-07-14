#include <jni.h>
#include <string.h>
#include <memory>
#include <opencv2/opencv.hpp>
#include <opencv2/core/mat.hpp>
#include "iqindex.h"
#include <android/log.h>

#define TAG "IQIndex NATIVE"

#define JNI_GLOBAL extern "C"

#ifdef _DEBUG
#define TRACE_ENTER __android_log_print(ANDROID_LOG_INFO, TAG, "%s", __PRETTY_FUNCTION__)
#else
#define TRACE_ENTER
#endif

static jclass clsRuntimeException;

JNI_GLOBAL jint JNI_OnLoad(JavaVM* vm, void* /*reserved*/)
{
    jint jversion = JNI_VERSION_1_4;
    JNIEnv *env;
    if (vm->GetEnv((void**)&env, jversion) != JNI_OK)
        return -1;

    clsRuntimeException = env->FindClass("java/lang/RuntimeException");

    // TODO: do native initialization here

    return jversion;
}

static void jthrow(JNIEnv *env)
{
    try
    {
        throw;
    }
    catch (cv::Exception &ex)
    {
        env->ThrowNew(clsRuntimeException, "cv::Exception [Exception in native code]");
    }
    catch (...)
    {
        env->ThrowNew(clsRuntimeException, "Unknown exception in native code");
    }
}

// Mat implementation

static Mat *mat(jlong nativeObj)
{
    return reinterpret_cast<Mat *>(nativeObj);
}

JNI_GLOBAL jlong JNICALL Java_com_iqengines_sdk_Mat_create_1n
  (JNIEnv *env, jobject, jstring file)
{
    const char *native_file = env->GetStringUTFChars(file, 0);
    Mat obj = cv::imread(native_file);
    env->ReleaseStringUTFChars(file, native_file);

    return reinterpret_cast<jlong>(new Mat(obj));
}

JNI_GLOBAL void JNICALL Java_com_iqengines_sdk_Mat_destroy_1n
  (JNIEnv *env, jobject, jlong nativeObj)
{
    delete mat(nativeObj);
}

JNI_GLOBAL jint JNICALL Java_com_iqengines_sdk_Mat_cols_1n
  (JNIEnv *env, jobject, jlong nativeObj)
{
    return mat(nativeObj)->cols;
}

JNI_GLOBAL jint JNICALL Java_com_iqengines_sdk_Mat_rows_1n
  (JNIEnv *env, jobject, jlong nativeObj)
{
    return mat(nativeObj)->rows;
}

JNI_GLOBAL jlong JNICALL Java_com_iqengines_sdk_Mat_submat_1n
  (JNIEnv *env, jobject, jlong nativeObj, jint x, jint y, jint width, jint height)
{
    Mat obj = (*mat(nativeObj))(Rect(x, y, width, height));
    return reinterpret_cast<jlong>(new Mat(obj));
}

JNI_GLOBAL jlong JNICALL Java_com_iqengines_sdk_Mat_resize_1n
  (JNIEnv *env, jobject, jlong nativeObj, jint width, jint height)
{
    Mat obj;
    cv::resize(*mat(nativeObj), obj, Size(width, height));
    return reinterpret_cast<jlong>(new Mat(obj));
}

// IQIndex implementation

static IQIndex *index(jlong nativeObj)
{
    return reinterpret_cast<IQIndex *>(nativeObj);
}

JNI_GLOBAL jlong Java_com_iqengines_sdk_IQLocal_nativeCreate(JNIEnv *env, jobject )
{
    return reinterpret_cast<jlong>(new IQIndex());
}

JNI_GLOBAL void Java_com_iqengines_sdk_IQLocal_nativeDestroy(JNIEnv *env, jobject , jlong nativeObj)
{
    delete index(nativeObj);
}

JNI_GLOBAL jint Java_com_iqengines_sdk_IQLocal_load(JNIEnv* env, jobject , jlong nativeObj, jstring indexPath, jstring imagesPath)
{
    TRACE_ENTER;

    try
    {
        const char *index_path = env->GetStringUTFChars(indexPath, 0);
        const char *images_path = env->GetStringUTFChars(imagesPath, 0);

        int result = index(nativeObj)->load(index_path, images_path);

        env->ReleaseStringUTFChars(indexPath, index_path);
        env->ReleaseStringUTFChars(imagesPath, images_path);

        return result;
    }
    catch (...)
    {
        jthrow(env);
    }
}

JNI_GLOBAL jint Java_com_iqengines_sdk_IQLocal_match(JNIEnv* env, jobject , jlong nativeObj, jlong matObj)
{
    TRACE_ENTER;
    try
    {
        return index(nativeObj)->match( *reinterpret_cast<Mat*>(matObj) );
    }
    catch (...)
    {
        jthrow(env);
    }
}

JNI_GLOBAL jint Java_com_iqengines_sdk_IQLocal_train(JNIEnv* env, jobject , jlong nativeObj)
{
    TRACE_ENTER;
    try
    {
        return index(nativeObj)->train();
    }
    catch (...)
    {
        jthrow(env);
    }
}

JNI_GLOBAL jint Java_com_iqengines_sdk_IQLocal_compute(JNIEnv* env, jobject , jlong nativeObj, jlong matObj, jstring arg1, jstring arg2 )
{
    TRACE_ENTER;

    try
    {
        const char *nativeArg1 = env->GetStringUTFChars(arg1, 0);
        const char *nativeArg2 = env->GetStringUTFChars(arg2, 0);

        int result = index(nativeObj)->compute( *reinterpret_cast<Mat *>(matObj), nativeArg1, nativeArg2);

        env->ReleaseStringUTFChars( arg1, nativeArg1 );
        env->ReleaseStringUTFChars( arg2, nativeArg2 );

        return result;
    }
    catch (...)
    {
        jthrow(env);
    }
}

JNI_GLOBAL jint Java_com_iqengines_sdk_IQLocal_getObjCount(JNIEnv *env, jobject , jlong nativeObj)
{
    TRACE_ENTER;
    try
    {
        return index(nativeObj)->obj_ids.size();
    }
    catch (...)
    {
        jthrow(env);
    }
}

JNI_GLOBAL jstring Java_com_iqengines_sdk_IQLocal_getObjId(JNIEnv *env, jobject , jlong nativeObj, jint idx)
{
    TRACE_ENTER;
    try
    {
        string const &id = index(nativeObj)->obj_ids[idx];
        return env->NewStringUTF(id.c_str());
    }
    catch (...)
    {
        jthrow(env);
    }
}

JNI_GLOBAL jstring Java_com_iqengines_sdk_IQLocal_getObjName(JNIEnv *env, jobject , jlong nativeObj, jstring objId)
{
    TRACE_ENTER;
    try
    {
        const char *id = env->GetStringUTFChars(objId, 0);

        typedef map<string, string> map_t;
        map_t::const_iterator it = index(nativeObj)->obj_id2name.find(id);
        env->ReleaseStringUTFChars(objId, id);

        if (it == index(nativeObj)->obj_id2name.end())
            return NULL;

        return env->NewStringUTF(it->second.c_str());
    }
    catch (...)
    {
        jthrow(env);
    }
}

JNI_GLOBAL jstring Java_com_iqengines_sdk_IQLocal_getObjMeta(JNIEnv *env, jobject , jlong nativeObj, jstring objId)
{
    TRACE_ENTER;
    try
    {
        const char *id = env->GetStringUTFChars(objId, 0);

        typedef map<string, string> map_t;
        map_t::const_iterator it = index(nativeObj)->obj_id2meta.find(id);
        env->ReleaseStringUTFChars(objId, id);

        if (it == index(nativeObj)->obj_id2name.end())
            return NULL;

        return env->NewStringUTF(it->second.c_str());
    }
    catch (...)
    {
        jthrow(env);
    }
}
