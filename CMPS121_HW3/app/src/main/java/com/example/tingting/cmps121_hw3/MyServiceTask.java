package com.example.tingting.cmps121_hw3;

import android.content.Context;
import android.util.Log;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class MyServiceTask implements Runnable {

    public static final String LOG_TAG = "MyService";
    private boolean running;
    private Context context;

    private Set<ResultCallback> resultCallbacks = Collections.synchronizedSet(
            new HashSet<ResultCallback>());
    private ConcurrentLinkedQueue<ServiceResult> freeResults =
            new ConcurrentLinkedQueue<>();

    public MyServiceTask(Context _context) {
        context = _context;
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            // TODO: sleep for 30 or not??
            Date d = new Date();
            didItMove(d);
        }
    }

    public void addResultCallback(ResultCallback resultCallback) {
        Log.i(LOG_TAG, "Adding result callback");
        resultCallbacks.add(resultCallback);
    }

    public void removeResultCallback(ResultCallback resultCallback) {
        Log.i(LOG_TAG, "Removing result callback");
        resultCallbacks.remove(resultCallback);
        freeResults.clear();
    }

    // Creates result bitmaps if they are needed.
    private void createResultsBuffer() {
        // I create some results to talk to the callback, so we can reuse these instead of creating new ones.
        // The list is synchronized, because integers are filled in the service thread,
        // and returned to the free pool from the UI thread.
        freeResults.clear();
        for (int i = 0; i < 10; i++) {
            freeResults.offer(new ServiceResult());
        }
    }

    // This is called by the UI thread to return a result to the free pool.
    public void releaseResult(ServiceResult r) {
        Log.i(LOG_TAG, "Freeing result holder for " + r.booleanValue);
        freeResults.offer(r);
    }

    public void stopProcessing() {
        running = false;
    }

    public void setTaskState(boolean b) {
        // Do something with b.
    }

    private void notifyResultCallback(boolean i) {
        if (!resultCallbacks.isEmpty()) {
            // If we have no free result holders in the buffer, then we need to create them.
            if (freeResults.isEmpty()) {
                createResultsBuffer();
            }
            ServiceResult result = freeResults.poll();
            // If we got a null result, we have no more space in the buffer,
            // and we simply drop the integer, rather than sending it back.
            if (result != null) {
                result.booleanValue = i;
                for (ResultCallback resultCallback : resultCallbacks) {
                    Log.i(LOG_TAG, "calling resultCallback for " + result.booleanValue);
                    resultCallback.onResultReady(result);
                }
            }
        }
    }

    public interface ResultCallback {
        void onResultReady(ServiceResult result);
    }

    // The actual didItMove function, which returns a true if it moved more than 30 seconds ago
    public boolean didItMove (Date f_time){
        Log.i(LOG_TAG, "checking didItmove()");
        boolean moved = false;
        Date d = new Date();
        synchronized (this) {
            // TODO: change both place back to 30!!!
            if (f_time != null && ((d.getTime() - f_time.getTime()) / 1000) > 5) {
                moved = true;
            }
        }
        notifyResultCallback(moved);
        return moved;
    }

}
