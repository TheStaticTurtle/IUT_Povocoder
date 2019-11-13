import java.awt.image.SampleModel;
import java.util.concurrent.CompletionException;

import static java.lang.System.exit;
import static java.lang.System.out;

public class Povocoder {

	// Processing SEQUENCE size (100 msec with 44100Hz samplerate)
	final static int SEQUENCE = StdAudio.SAMPLE_RATE/10;
	// Overlapping size (20 msec)
	final static int OVERLAP = SEQUENCE/5 ;
	// Best OVERLAP offset seeking window (15 msec)
	final static int SEEK_WINDOW = 3*OVERLAP/4;

	public static void main(String[] args) {
		if (args.length < 2)
		{
			System.out.println("usage: povocoder input.wav freqScale\n");
			exit(1);
		}

		try
		{
			String wavInFile = args[0];
			double freqScale = Double.valueOf(args[1]);
			String outPutFile= wavInFile.split("\\.")[0] + "_" + freqScale +"_";


			// Open input .wev file
			double[] inputWav = StdAudio.read(wavInFile);

			// Resample test
			double[] newPitchWav = resample(inputWav, freqScale);
			StdAudio.save(outPutFile+"Resampled.wav", newPitchWav);

			// Simple dilatation
			double[] outputWav   = vocodeSimple(newPitchWav, 1.0/freqScale);
			StdAudio.save(outPutFile+"Simple.wav", outputWav);

			// Simple dilatation with overlaping
			//outputWav = vocodeSimpleOver(newPitchWav, 1.0/freqScale);
			//StdAudio.save(outPutFile+"SimpleOver.wav", outputWav);

			// Simple dilatation with overlaping and maximum cross correlation search
			//outputWav = vocodeSimpleOverCross(newPitchWav, 1.0/freqScale);
			//StdAudio.save(outPutFile+"SimpleOverCross.wav", outputWav);

			//joue(outputWav);

			// Some echo above all
			//outputWav = echo(outputWav, 100, 0.7);
			//StdAudio.save(outPutFile+"SimpleOverCrossEcho.wav", outputWav);

		}
		catch (Exception e)
		{
			System.out.println("Error: "+ e.toString());
		}
	}

	static double[] resample(double[] input,double freqScale) {
		double scale =1;
		if( freqScale > 1) {
			scale = (freqScale - 1)/freqScale;
			scale = 1 - scale;
		}	
		if( freqScale < 1) {
			scale = (freqScale + 1)/freqScale;
			scale = 1 - scale;
			if(scale < 0) { scale *= -1; }
		}


		System.out.println("freqScale: "+freqScale);
		System.out.println("scale:"+ scale);

		int iOut = 0;
		double counter = 0;
		for (int i=0; i< input.length; i++) {
			while(counter > 1) { 
				iOut++; 
				counter-=1;
			}
			counter += scale;
		}

		double[] output = new double[iOut+1];
		System.out.println("input.length: "+input.length);
		System.out.println("iOut+1: "+ (iOut+1));
		System.out.println("input.length / 44100: "+ (input.length / StdAudio.SAMPLE_RATE));
		System.out.println("(iOut+1) / 44100: "+ ((iOut+1) / StdAudio.SAMPLE_RATE));

		counter=0;
		iOut = 0;
		for (int i=0; i< input.length; i++) {
			while(counter > 1) {
				output[iOut] = input[i];
				counter-=1;
				iOut++;
			}
			counter += scale;
		}
		System.out.println("");
		System.out.println("");
		System.out.println("");
		return output;
	}

	public static double[] vocodeSimple(double[] input, double timeScale){
		double seqDuration = 0.01; //ms
		int seqSize = (int)(StdAudio.SAMPLE_RATE * seqDuration);
		int jumpSize = (int)(seqSize * timeScale);
		double ratio = (double)jumpSize / (double)seqSize;

		System.out.println("ratio: "+ ratio);
		System.out.println("seqSize len: "+ ((double)seqSize / StdAudio.SAMPLE_RATE ));
		System.out.println("jumpSize len: "+ ((double)jumpSize / StdAudio.SAMPLE_RATE ));


		int length = (int)((seqSize) * (input.length / jumpSize));
			
		System.out.println("input.length / 44100: "+ (input.length / StdAudio.SAMPLE_RATE));
		System.out.println("length / 44100: "+ (length / StdAudio.SAMPLE_RATE));
		double[] output = new double[length]; 

		if(ratio > 1) {

			int i2 = 0;
			for (int i = 0; i < length; i+=seqSize) {
				
				for (int j = 0; j < seqSize; j++) {
					output[i+j] = input[  Math.min(i2+j,input.length-1) ];
				}

				i2+= jumpSize;

			}
			return output;

		} else if (ratio < 1) {

			int i2 = 0;
			for (int i = 0; i < length; i+=seqSize) {
				
				for (int j = 0; j < seqSize; j++) {
					output[i+j] = input[  Math.min(i2+j,input.length-1) ];
				}

				int left = jumpSize - seqSize;

				for (int j = 0; j < left; j++) {
					output[i+j+seqSize] = input[  Math.min(i2+j+seqSize,input.length-1) ];
				}

				i2+= jumpSize;

			}
			return output;
		} else {
			return output;
		}
	}

}
