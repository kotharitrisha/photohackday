package com.iqengines.sdk;

public class Mat {
    
    public long nativeObj;
    
    private native long create_n(String file);
    private native void destroy_n(long nativeObj);
    private native int cols_n(long nativeObj);
    private native int rows_n(long nativeObj);
    private native long submat_n(long nativeObj, int x, int y, int width, int height);
    private native long resize_n(long nativeObj, int width, int height);
    
    public Mat(String file) {
        nativeObj = create_n(file);
    }
    
    public Mat(long nativeObj) {
        this.nativeObj = nativeObj;
    }
    
    public void destroy() {
        destroy_n(nativeObj);
        nativeObj = 0;
    }
    
    @Override
    protected void finalize() throws Throwable {
        destroy();
        super.finalize();
    }
    
    public int cols() {
        return cols_n(nativeObj);
    }
    
    public int rows() {
        return rows_n(nativeObj);
    }
    
    public Mat submat(int x, int y, int width, int height) {
        long newObj = submat_n(nativeObj, x, y, width, height);
        return new Mat(newObj);
    }
    
    public Mat resize(int width, int height) {
        long newObj = resize_n(nativeObj, width, height);
        return new Mat(newObj);
    }
    
}
