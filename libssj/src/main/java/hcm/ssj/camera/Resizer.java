/*
 * ImageResizer.java
 * Copyright (c) 2017
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken, Simon Flutura
 * *****************************************************
 * This file is part of the Social Signal Interpretation for Java (SSJ) framework
 * developed at the Lab for Human Centered Multimedia of the University of Augsburg.
 *
 * SSJ has been inspired by the SSI (http://openssi.net) framework. SSJ is not a
 * one-to-one port of SSI to Java, it is an approximation. Nor does SSJ pretend
 * to offer SSI's comprehensive functionality and performance (this is java after all).
 * Nevertheless, SSJ borrows a lot of programming patterns from SSI.
 *
 * This library is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3 of the License, or any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this library; if not, see <http://www.gnu.org/licenses/>.
 */

package hcm.ssj.camera;

import android.graphics.Bitmap;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Log;
import hcm.ssj.core.Transformer;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.ImageStream;
import hcm.ssj.core.stream.Stream;

/**
 * Transformer that re-sizes image to necessary dimensions.
 * @author Vitaly
 */

public class Resizer extends Transformer
{
	public class Options extends OptionList
	{
		public final Option<Integer> cropSize = new Option<>("cropSize", 224, Integer.class, "size of the cropped image");
		public final Option<Boolean> maintainAspect = new Option<>("maintainAspect", true, Boolean.class, "maintain aspect ration");

		private Options()
		{
			addOptions();
		}
	}

	public final Options options = new Options();
	private CameraImageResizer imageResizer;

	private int width;
	private int height;
	private int cropSize = 224;
	private int counter = 0;

	private int[] intValues;

	public Resizer()
	{
		_name = "Resizer";
	}

	@Override
	public void enter(Stream[] stream_in, Stream stream_out)
	{
		// Get image dimensions
		width = ((ImageStream) stream_in[0]).width;
		height = ((ImageStream) stream_in[0]).height;

		// Get user options
		boolean maintainAspect = options.maintainAspect.get();
		cropSize = options.cropSize.get();


		if (cropSize <= 0 || cropSize >= width || cropSize >= height)
		{
			Log.d("Invalid crop size. Crop size must be smaller than width and height.");
			return;
		}

		imageResizer = new CameraImageResizer(width, height, cropSize, maintainAspect);
		intValues = new int[cropSize * cropSize];
	}

	@Override
	public void transform(Stream[] stream_in, Stream stream_out)
	{
		if (cropSize <= 0 || cropSize >= width || cropSize >= height)
		{
			Log.d("Invalid crop size. Crop size must be smaller than width and height.");
			return;
		}

		// Convert byte array to integer array
		int[] rgb = CameraUtil.decodeBytes(stream_in[0].ptrB(), width, height);

		// Resize image and write byte array to output buffer
		Bitmap cropped = imageResizer.resizeImage(rgb);
		bitmapToByteArray(cropped, stream_out.ptrB());
	}

	@Override
	public int getSampleDimension(Stream[] stream_in)
	{
		return stream_in[0].dim;
	}

	@Override
	public int getSampleBytes(Stream[] stream_in)
	{
		return stream_in[0].bytes;
	}

	@Override
	public Cons.Type getSampleType(Stream[] stream_in)
	{
		return Cons.Type.IMAGE;
	}

	@Override
	public int getSampleNumber(int sampleNumber_in)
	{
		return sampleNumber_in;
	}

	@Override
	protected void describeOutput(Stream[] stream_in, Stream stream_out)
	{
		stream_out.desc = new String[] { "Cropped video" };
		((ImageStream) stream_out).width = cropSize;
		((ImageStream) stream_out).height = cropSize;
		((ImageStream) stream_out).format = 0x29; //ImageFormat.FLEX_RGB_888;
	}

	/**
	 * Converts bitmap to corresponding byte array and writes it
	 * to the output buffer.
	 *
	 * @param bitmap Bitmap to convert to byte array.
	 * @param out Output buffer.
	 */
	private void bitmapToByteArray(Bitmap bitmap, byte[] out)
	{
		bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());

		for (int i = 0; i < bitmap.getWidth() * bitmap.getHeight(); ++i)
		{
			final int pixel = intValues[i];
			out[i * 3] = (byte)((pixel >> 16) & 0xFF);
			out[i * 3 + 1] = (byte)((pixel >> 8) & 0xFF);
			out[i * 3 + 2] = (byte)(pixel & 0xFF);
		}
	}
}
