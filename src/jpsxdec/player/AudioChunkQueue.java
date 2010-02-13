/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package jpsxdec.player;


public class AudioChunkQueue extends MultiStateBlockingQueue<IDecodableAudioChunk> {

    private static final boolean DEBUG = true;
    
    VideoProcessor _vidProd;

    public AudioChunkQueue(int iCapacity, VideoProcessor vidProc) {
        super(iCapacity);
        _vidProd = vidProc;
    }

    @Override
    protected IDecodableAudioChunk dequeue() {
        IDecodableAudioChunk chnk = super.dequeue();
        if (_vidProd != null && isEmpty()) {
            if (DEBUG) System.out.println(Thread.currentThread().getName() + " audio queue empty, telling video queue to overwrite");
            _vidProd.overwriteWhenFull();
        }
        return chnk;
    }

    @Override
    protected void enqueue(IDecodableAudioChunk o) {
        boolean blnWasEmpty = isEmpty();
        super.enqueue(o);
        if (_vidProd != null && blnWasEmpty) {
            if (DEBUG) System.out.println(Thread.currentThread().getName() + " audio queue is no longer empty, telling video queue to block");
            _vidProd.play();
        }
    }
}
