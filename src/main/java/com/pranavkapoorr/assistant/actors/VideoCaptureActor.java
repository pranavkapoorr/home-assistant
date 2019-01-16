package com.pranavkapoorr.assistant.actors;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import javax.swing.ImageIcon;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import akka.actor.AbstractActor;
import akka.actor.ActorRef;
import akka.actor.Props;
import akka.util.ByteString;

public class VideoCaptureActor extends AbstractActor{
	private final static Logger log = LogManager.getLogger(VideoCaptureActor.class);
	public volatile static boolean isClientOn;
	public volatile static boolean isServerOn;
	private  ActorRef communicationActor = null;
	private ActorRef ref;
	private VideoCapture cap;
	static{
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	}
	private VideoCaptureActor(ActorRef comActor) {
	        this.communicationActor = comActor;
	         cap = new VideoCapture("http://192.168.0.166:8089/video");
	         ref = context().actorOf(xyz.props());
	}
	  
	public static Props props(ActorRef comActor) {
	    return Props.create(VideoCaptureActor.class,comActor);
	}

	  @Override
	public void preStart() throws Exception {
	      isClientOn = false;
	      isServerOn = false;
		  log.info("starting Video Capture Actor");
		  getSelf().tell("start", getSelf());
	}
	@Override
	public Receive createReceive() {
		return receiveBuilder()
		        .match(byte[].class, b->{
		            log.trace("received myself");
		        })
				.match(String.class,s->{
				    if(s.equalsIgnoreCase("start")){
				        getVideoRunning();
				    }else{
				        log.trace(s);
				    }
				    
				})
				.build();
	}
	private BufferedImage mat2Image(Mat original){
		BufferedImage image = null;
		int width = original.width(), height = original.height(), channels = original.channels();
		byte[] sourcePixels = new byte[width * height * channels];
		original.get(0, 0, sourcePixels);
		
		if (original.channels() > 1)
		{
			image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		}
		else
		{
			image = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
		}
		final byte[] targetPixels = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
		System.arraycopy(sourcePixels, 0, targetPixels, 0, sourcePixels.length);
		
		return image;
	}
	private void getVideoRunning() throws IOException{
	    log.trace("starting video");
			while(true){	
				Mat frame = new Mat();
	          if (cap.read(frame)){
	            	BufferedImage image = mat2Image(frame);
	            	if(isClientOn || isServerOn){
	            	communicationActor.tell((ByteString.fromArray(TcpConnectionHandlerActor.Image2Array(image))),getSelf());
	            		//log.info("sending frame");
	            	}
	            	MainFrame.screen1.setIcon(new ImageIcon(image));
	            	
	            }	
			}
	}
	@Override
	public void postStop() throws Exception {
		log.info("stopping Video Capture Actor");
	}
	static class xyz extends AbstractActor{

        @Override
        public Receive createReceive() {
            return receiveBuilder().match(String.class, s->log.info(s)).build();
        }
        public static Props props() {
            return Props.create(xyz.class);
        }
	}
}
