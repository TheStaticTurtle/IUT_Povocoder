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
			String outputFile= wavInFile.split("\\.")[0] + "_" + freqScale +"_";

			// Open input .wav file
			double[] inputWav = StdAudio.read(wavInFile);

			// Resample test
			double[] newPitchWav = resample(inputWav, freqScale);
			StdAudio.save(outputFile+"Resampled.wav", newPitchWav);

			// Simple dilatation
			double[] outputWav = vocodeSimple(newPitchWav, 1.0/freqScale);
			StdAudio.save(outputFile+"Simple.wav", outputWav);

			// Simple dilatation with overlaping
			outputWav = vocodeSimpleOver(newPitchWav, 1.0/freqScale);
			StdAudio.save(outputFile+"SimpleOver.wav", outputWav);

			// Simple dilatation with overlaping and maximum cross correlation search
			//outputWav = vocodeSimpleOverCross(newPitchWav, 1.0/freqScale);
			//StdAudio.save(outputFile+"SimpleOverCross.wav", outputWav);

			//joue(outputWav);

			// Some echo above all
			//double[] outputWav = echo(outputWav, 1000, 0.7);
			//StdAudio.save(outputFile+"SimpleOverCrossEcho.wav", outputWav);
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

	static double map(double x, double in_min, double in_max, double out_min, double out_max) {
		return (x - in_min) * (out_max - out_min) / (in_max - in_min) + out_min;
	}

	public static double[] fadeIn(double[] input) {
		int maxI = input.length;

		for (int i = 0 ; i < maxI ; i++) {
			double multiplier = map(i, 0, maxI, 0.0, 1.0);
			input[i] = input[i] * multiplier;
		}
		return input;
	}

	public static double[] fadeOut(double[] input) {
		int maxI = input.length;

		for (int i = 0 ; i < maxI ; i++) {
			double multiplier = map(i, 0, maxI, 1.0, 0.0);
			input[i] = input[i] * multiplier;
		}
		return input;
	}


	public static double[] vocodeSimple(double[] input, double timeScale){
		// double seqDuration = 0.01; //ms
		int seqSize = SEQUENCE;//(int)(StdAudio.SAMPLE_RATE * seqDuration);
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

	static double[] vocodeSimpleOver(double[] input, double timeScale){
		int seqSize = SEQUENCE; //(int)(StdAudio.SAMPLE_RATE * seqDuration);
		int fadeSize = OVERLAP; //(int)(StdAudio.SAMPLE_RATE * fadeDuration);

		int jumpSize = (int)(seqSize * timeScale);

		int length = (int)((seqSize - fadeSize) * (input.length / jumpSize));

		double[] output = new double[length];

		if(timeScale > 1) {
			int i2 = 0;
			double[] fade = new double[fadeSize];
			for (int i = 0; i < length; i += seqSize - fadeSize) { //Pour chaque séquence du fichier

				// -------------------------------Fade In------------------------------
				double[] fade2 = new double[fadeSize];
				for (int j = 0; j < fade2.length; j++) { //On rempli le tableau des valeurs a fade
					fade2[j] = input[Math.min(i2 + j, input.length-1)];
				}
				//fade2 = fadeIn(fade2); //On applique l'effet

				double[] newFade = new double[fadeSize];
				for (int j = 0; j < fadeSize; j++) { // On mixe les deux valeurs
					double balance = map(j, 0, fadeSize, 0., 1.);
					//newFade[j] = (fade[j] * (1-balance) + fade2[j] * balance);
					//System.out.println(Double.toString(fade[j]) + " -- " + Double.toString(fade2[j]) + " -- " + Double.toString((fade[j] + fade2[j]) / 2));
					newFade[j] = (fade[j] + fade2[j]) / 2;
				}

				for (int j = 0; j < fadeSize; j++) { //On remplace les valeurs dans le son final
					output[i+j] = newFade[j];
				}


				// -------------------------------Milieu------------------------------
				for (int j = fadeSize; j < seqSize - fadeSize; j++) { //Pour chaque valeur dans la séquence
					output[i+j] = input[Math.min(i2 + j + fadeSize, input.length-1)]; //Min dans le cas ou on dépasse la fin du tableau
				}


				// -------------------------------Fade Out------------------------------
				for (int j = 0; j < fadeSize; j++) { //On rempli le tableau des valeurs a fade
					fade[j] = input[Math.min(i2 + j + seqSize - fadeSize, input.length-1)];
				}
				//fade = fadeOut(fade); //On applique l'effet

				i2 += jumpSize; //On jump
			}
			return output;

		} else if (timeScale < 1) {
			return output;
		} else {
			return output;
		}
	}

	static double[] echo(double[] input, double delayMS, double attn) {
		if(attn <= 0) { return input; }
		int padding = StdAudio.SAMPLE_RATE * (int)delayMS / 1000;
		int outputLenght = input.length + padding;

		double[] output = new double[outputLenght];
		System.out.println("Echo output len: "+ outputLenght);

		//Peut pas faire output = input car sa copie aussi la taille
		for (int i=0;i<input.length; i++) {
			output[i] = input[i];
		}

		for (int i=0; i< input.length; i++ ) {
			int o = i+padding; //Math.min(i+padding , output.length-1);
			output[o] = (output[o] + input[i] * attn) / 2;
		}

		return output;
	}

}
