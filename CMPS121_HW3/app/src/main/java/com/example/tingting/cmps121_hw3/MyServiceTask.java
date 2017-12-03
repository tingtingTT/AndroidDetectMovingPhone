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
    private AtomicLong firstAccelerateTime;
    private AtomicLong startTime;


    private Set<ResultCallback> resultCallbacks = Collections.synchronizedSet(
            new HashSet<ResultCallback>());
    private ConcurrentLinkedQueue<ServiceResult> freeResults =
            new ConcurrentLinkedQueue<>();

    public MyServiceTask(Context _context) {
        context = _context;
        firstAccelerateTime = null;
        startTime = null;
    }

    @Override
    public void run() {
        running = true;

        // set start time when app is open
        Date date = new Date();
        startTime = new AtomicLong(date.getTime());

        while (running) {
            // Sleep for 30 seconds
            try {
                Thread.sleep(5000);
            } catch (Exception e) {
                e.getLocalizedMessage();
            }
            // check if the phone is moved after 30 seconds
            boolean moved = didItMove();
            // Sends it to the UI thread in MainActivity (if MainActivity
            // is running).
            Log.i(LOG_TAG, "Getting moved result: " + moved);

            // if the phone is moved, sleep for 30 seconds and then display
            if (moved == true) {
                // TODO: change it back to 30 seconds
                try {
                    Thread.sleep(5000);
                } catch (Exception e) {
                    e.getLocalizedMessage();
                }
            }
            notifyResultCallback(moved);
        }
    }

    public void addResultCallback(ResultCallback resultCallback) {
        Log.i(LOG_TAG, "Adding result callback");
        resultCallbacks.add(resultCallback);
    }

    public void removeResultCallback(ResultCallback resultCallback) {
        Log.i(LOG_TAG, "Removing result callback");
        // We remove the callback...
        resultCallbacks.remove(resultCallback);
        // ...and we clear the list of results.
        // Note that this works because, even though mResultCallbacks is a synchronized set,
        // its cardinality should always be 0 or 1 -- never more than that.
        // We have one viewer only.
        // We clear the buffer, because some result may never be returned to the
        // free buffer, so using a new set upon reattachment is important to avoid
        // leaks.
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

    public synchronized boolean didItMove() {
        // TODO: make a function to check the movement of the phone
        // TODO: value of d should be start time, it should be set earlier
        // TODO: change to sync

        Date date = new Date();
        AtomicLong d = new AtomicLong(date.getTime());
        boolean moved = false;
        if (firstAccelerateTime != null && (d.get() - firstAccelerateTime.get()) / 1000 > 30) {
            // if phone is moved, set firstAccelerate time to current time
            moved = true;
            firstAccelerateTime = d;
        }
        return moved;
    }

    public synchronized void resetData(AtomicLong d) {
        startTime = d;
        firstAccelerateTime = null;
        Log.i(LOG_TAG, "clear button pressed");
    }

    // TODO: onSensorChange:
    // synchronized(this) {
           // update
    //   }

}
