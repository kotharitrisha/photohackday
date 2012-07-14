LOCAL_PATH := $(call my-dir)

OPENCV_PACKAGE_DIR := $(LOCAL_PATH)/../../prebuilt/OpenCV-2.3.1

include $(CLEAR_VARS)
LOCAL_MODULE := iqindex
LOCAL_SRC_FILES := ./libiqindex.so
include $(PREBUILT_SHARED_LIBRARY)

include $(CLEAR_VARS)

LOCAL_MODULE    := iqengines-sdk

LOCAL_C_INCLUDES := \
	$(OPENCV_PACKAGE_DIR)/include \
	$(IQE_DIR)

LOCAL_SRC_FILES := \
	main.cpp

LOCAL_SHARED_LIBRARIES := iqindex

LOCAL_LDLIBS := -llog

include $(BUILD_SHARED_LIBRARY)
