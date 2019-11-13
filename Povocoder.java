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
			//StdAudio.save(outPutFile+"Simple.wav", outputWav);

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
		double scale = 1;
		if( freqScale > 1) {
			scale = (freqScale - 1)/freqScale;
		}	
		if( freqScale < 1) {
			scale = (freqScale + 1)/freqScale;
		}	

		System.out.println(freqScale);
		System.out.println(scale);


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

		return output;
	}

	public static double[] vocodeSimple(double[] input, double timeScale){
		double[] output = new double[input.length];
		for (int i = 0; i < input.length; i++) {
			
		}
		return output;
	}

}
