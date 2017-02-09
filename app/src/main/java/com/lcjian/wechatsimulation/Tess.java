package com.lcjian.wechatsimulation;

//import com.googlecode.tesseract.android.ResultIterator;
//import com.googlecode.tesseract.android.TessBaseAPI;

//import java.io.File;

//import timber.log.Timber;

public class Tess {

//    private static final String TESS_PATH = Environment.getExternalStorageDirectory() + "/tess";
//
//    private static final String DEFAULT_LANGUAGE = "chi_sim";
//
//    private TessBaseAPI mTess;
//
//    public Tess() {
//        mTess = new TessBaseAPI();
//        mTess.init(TESS_PATH, DEFAULT_LANGUAGE);
//    }
//
//    public Rect find(File file, String str) {
//        mTess.setImage(file);
//        mTess.getUTF8Text();
//        ResultIterator resultIterator = mTess.getResultIterator();
//        resultIterator.begin();
//        while (resultIterator.next(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE)) {
//            String text = resultIterator.getUTF8Text(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE);
//            Timber.d(text);
//
//            if (!TextUtils.isEmpty(text) && text.contains(str)) {
//                return resultIterator.getBoundingRect(TessBaseAPI.PageIteratorLevel.RIL_TEXTLINE);
//            }
//        }
//        resultIterator.delete();
//        return null;
//    }
//
//    public void end() {
//        mTess.end();
//    }
}
