package mpicbg.stitching;

import java.util.ArrayList;

import process.OverlayFusion;

import mpicbg.imglib.algorithm.fft.PhaseCorrelation;
import mpicbg.imglib.algorithm.fft.PhaseCorrelationPeak;
import mpicbg.imglib.algorithm.scalespace.DifferenceOfGaussianPeak;
import mpicbg.imglib.algorithm.scalespace.SubpixelLocalization;
import mpicbg.imglib.image.Image;
import mpicbg.imglib.type.numeric.RealType;
import mpicbg.imglib.type.numeric.integer.UnsignedByteType;
import mpicbg.imglib.type.numeric.integer.UnsignedShortType;
import mpicbg.imglib.type.numeric.real.FloatType;
import mpicbg.imglib.util.Util;
import mpicbg.models.InvertibleBoundable;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;

/**
 * Pairwise Stitching of two ImagePlus using ImgLib1 and PhaseCorrelation
 * 
 * @author Stephan Preibisch (stephan.preibisch@gmx.de)
 *
 */
public class StitchingImgLib 
{

	public StitchingImgLib( final ImagePlus imp1, final ImagePlus imp2, final StitchingParameters params )
	{
		//
		// the ugly but correct way into generic programming...
		//
		if ( imp1.getType() == ImagePlus.GRAY32 )
		{
			final FloatType type1 = new FloatType();
			
			if ( imp2.getType() == ImagePlus.GRAY32 )
				performStitching( type1, new FloatType(), imp1, imp2, params );
			else if ( imp2.getType() == ImagePlus.GRAY16 )
				performStitching( type1, new UnsignedShortType(), imp1, imp2, params );
			else if ( imp2.getType() == ImagePlus.GRAY8 )
				performStitching( type1, new UnsignedByteType(), imp1, imp2, params );
			else
				IJ.log( "Unknown image type: " + imp2.getType() );
		}
		else if ( imp1.getType() == ImagePlus.GRAY16 )
		{
			final UnsignedShortType type1 = new UnsignedShortType();
			
			if ( imp2.getType() == ImagePlus.GRAY32 )
				performStitching( type1, new FloatType(), imp1, imp2, params );
			else if ( imp2.getType() == ImagePlus.GRAY16 )
				performStitching( type1, new UnsignedShortType(), imp1, imp2, params );
			else if ( imp2.getType() == ImagePlus.GRAY8 )
				performStitching( type1, new UnsignedByteType(), imp1, imp2, params );
			else
				IJ.log( "Unknown image type: " + imp2.getType() );
		}
		else if ( imp1.getType() == ImagePlus.GRAY8 )
		{
			final UnsignedByteType type1 = new UnsignedByteType();
			
			if ( imp2.getType() == ImagePlus.GRAY32 )
				performStitching( type1, new FloatType(), imp1, imp2, params );
			else if ( imp2.getType() == ImagePlus.GRAY16 )
				performStitching( type1, new UnsignedShortType(), imp1, imp2, params );
			else if ( imp2.getType() == ImagePlus.GRAY8 )
				performStitching( type1, new UnsignedByteType(), imp1, imp2, params );
			else
				IJ.log( "Unknown image type: " + imp2.getType() );
		}
		else
		{
			IJ.log( "Unknown image type: " + imp1.getType() );			
		}
	}
	
	protected < T extends RealType<T>, S extends RealType<S> > void performStitching( final T type1, final S type2, final ImagePlus imp1, final ImagePlus imp2, final StitchingParameters params )
	{
		
	}
	
	public static < T extends RealType<T>, S extends RealType<S> > float[] computePhaseCorrelation( final Image<T> img1, final Image<S> img2, final int numPeaks, final boolean subpixelAccuracy )
	{
		final PhaseCorrelation< T, S > phaseCorr = new PhaseCorrelation<T, S>( img1, img2 );
		phaseCorr.setInvestigateNumPeaks( numPeaks );
		
		if ( subpixelAccuracy )
			phaseCorr.setKeepPhaseCorrelationMatrix( true );
		
		phaseCorr.setComputeFFTinParalell( true );
		phaseCorr.process();

		// result
		final PhaseCorrelationPeak pcp = phaseCorr.getShift();
		final float[] shift = new float[ img1.getNumDimensions() ];
		IJ.log( "Non subresolution shift: " + pcp );
		
		if ( subpixelAccuracy )
		{
			final Image<FloatType> pcm = phaseCorr.getPhaseCorrelationMatrix();		
		
			final ArrayList<DifferenceOfGaussianPeak<FloatType>> list = new ArrayList<DifferenceOfGaussianPeak<FloatType>>();		
			final Peak p = new Peak( pcp );
			list.add( p );
					
			final SubpixelLocalization<FloatType> spl = new SubpixelLocalization<FloatType>( pcm, list );
			final boolean move[] = new boolean[ pcm.getNumDimensions() ];
			for ( int i = 0; i < pcm.getNumDimensions(); ++i )
				move[ i ] = false;
			spl.setCanMoveOutside( true );
			spl.setAllowedToMoveInDim( move );
			spl.setMaxNumMoves( 0 );
			spl.setAllowMaximaTolerance( false );
			spl.process();
			
			final Peak peak = (Peak)list.get( 0 );
			
			for ( int d = 0; d < img1.getNumDimensions(); ++d )
				shift[ d ] = peak.getPCPeak().getPosition()[ d ] + peak.getSubPixelPositionOffset( d );
			
			IJ.log( "subpixel-resolution shift: " + Util.printCoordinates( shift ) + ", phaseCorrelationPeak = " + p.getValue() );
			pcm.close();
		}
		else
		{
			for ( int d = 0; d < img1.getNumDimensions(); ++d )
				shift[ d ] = pcp.getPosition()[ d ];
		}
		
		return shift;
	}
	
	/**
	 * return an {@link Image} of a {@link RealType} as input for the PhaseCorrelation. If no rectangular roi
	 * is selected, it will only wrap the existing ImagePlus!
	 * 
	 * @param targetType - which {@link RealType}
	 * @param imp - the {@link ImagePlus}
	 * 
	 * @return - the {@link Image}
	 */
	public static <T extends RealType<T>> Image<T> getImage( final T targetType, final ImagePlus imp )
	{
		// first test the roi
		final Roi roi = getOnlyRectangularRoi( imp );

		return null;
	}
	
	protected static Roi getOnlyRectangularRoi( final ImagePlus imp )
	{
		Roi roi = imp.getRoi();
		
		// we can only do rectangular rois
		if ( roi != null && roi.getType() == Roi.RECTANGLE )
		{
			IJ.log( "WARNING: roi for " + imp.getTitle() + " is not a rectangle, we have to ignore it." );
			roi = null;
		}

		return roi;
	}
}
