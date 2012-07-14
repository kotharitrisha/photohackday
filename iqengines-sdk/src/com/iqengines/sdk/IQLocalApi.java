package com.iqengines.sdk;

import java.io.File;
import java.util.List;

import android.content.res.Resources;

import com.iqengines.sdk.IQLocal.OnReady;

public interface IQLocalApi {

	public abstract void destroy();

	public abstract int match(Mat img);

	public abstract void match(Mat img, OnReady callback);

	public abstract int compute(Mat img, String arg1, String arg2);

	public abstract void compute(Mat img, String arg1, String arg2,
			OnReady callback);

	public abstract int load(File index, File images);

	public abstract int load(String indexPath, String imagesPath);

	public abstract void load(File index, File images, OnReady callback);

	public abstract void load(String indexPath, String imagesPath,
			OnReady callback);

	public abstract int train();

	public abstract void train(OnReady callback);

	public abstract void init(Resources res, File appDataDir, OnReady callback);

	public abstract void init(Resources res, File appDataDir);

	public abstract String getObjName(String objId);

	public abstract String getObjMeta(String objId);

	public abstract List<String> getObjIds();

}