package com.lcjian.wechatsimulation;

import android.os.Environment;
import android.util.Log;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.debugger.AbstractDebugger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Debugger extends AbstractDebugger {

    private final DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance();

    public Debugger(XMPPConnection connection, Writer writer, Reader reader) {
        super(connection, writer, reader);
    }

    @Override
    protected void log(String logMessage) {
        String formattedDate;
        synchronized (dateFormat) {
            formattedDate = dateFormat.format(new Date());
        }
        try {
            FileWriter fw = new FileWriter(new File(Environment.getExternalStorageDirectory(), "smack.log.txt"), true);
            PrintWriter pw = new PrintWriter(fw);
            pw.println(formattedDate + ' ' + logMessage);
            pw.close();
            Log.d("SMACK", logMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void log(String logMessage, Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        log(logMessage + sw);
    }
}
