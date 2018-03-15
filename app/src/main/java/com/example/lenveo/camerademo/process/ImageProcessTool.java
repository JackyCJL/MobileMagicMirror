package com.example.lenveo.camerademo.process;

import android.graphics.Bitmap;
import android.util.Log;

import com.example.lenveo.camerademo.common.ImageUtils;

import static java.lang.Math.log;
import static java.lang.Math.pow;

/**
 * Created by Lenveo on 2018/3/7.
 */

public class ImageProcessTool {
    private int mWidth;
    private int mHeight;
    private Bitmap srcbmp;

    public ImageProcessTool(Bitmap bmp){
        srcbmp = bmp;
        mWidth = bmp.getWidth();
        mHeight = bmp.getHeight();
    }

    // DehazeHeavy
    //一种夜间增强算法

    public class RetinexParams{
        public int scale;   /* 最大Retinex尺度 */
        public int nscales; /* 尺度个数        */
        public int scales_mode;   /* Retinex尺度计算模式，有3种：UNIFORM, LOW, HIGH */
        public float cvar;         /* 用于调整色彩动态范围的方差的倍乘系数           */
        public RetinexParams(int scale, int nscales, int scales_mode, float cvar){
            this.scale = scale;
            this.nscales = nscales;
            this.scales_mode = scales_mode;
            this.cvar = cvar;
        }
    }
    private final int MAX_RETINEX_SCALES = 8;        /* Retinex最多可采用的尺度的数目 */
    private final int MIN_GAUSSIAN_SCALE = 16;      /* 最小Gaussian尺度 */
    private final int MAX_GAUSSIAN_SCALE = 250;     /* 最大Gaussian尺度 */

    /* 3种Retinex尺度计算模式，均匀，低和高，它们决定RetinexScales中的尺度数据 */
    private final int RETINEX_UNIFORM = 0;
    private final int RETINEX_LOW = 1;
    private final int RETINEX_HIGH = 2;
    private RetinexParams rvals;
    private float[] RetinexScales = new float[MAX_RETINEX_SCALES];

    public void retinex_scales_distribution(float[] scales, int nscales, int mode, int s){
        if (nscales == 1)
        { /* For one filter we choose the median scale */
            scales[0] = (float)s / 2;
        }
        else if (nscales == 2)
        { /* For two filters we choose the median and maximum scale */
            scales[0] = (float)s / 2;
            scales[1] = (float)s;
        }
        else {
            float size_step = (float) s / (float) nscales;
            int i;

            switch (mode) {
                case RETINEX_UNIFORM:
                    for (i = 0; i < nscales; ++i)
                        scales[i] = 2.0f + (float) i * size_step;
                    break;

                case RETINEX_LOW:
                    size_step = (float) log(s - 2.0f) / (float) nscales;
                    for (i = 0; i < nscales; ++i)
                        scales[i] = 2.0f + (float) pow(10, (i * size_step) / log((double)10));
                    break;

                case RETINEX_HIGH:
                    size_step = (float) log(s - 2.0) / (float) nscales;
                    for (i = 0; i < nscales; ++i)
                        scales[i] = s - (float) pow(10, (i * size_step) / log((double)10));
                    break;

                default:
                    break;
            }
        }
    }

    public class gauss3_coefs{
        public int N;
        public float sigma;
        public double B;
        public double[] b;
        public gauss3_coefs(){
            b = new double[4];
        }
    }

    public void compute_coefs3(gauss3_coefs c, float sigma){
        float q, q2, q3;
        q = 0;
        if (sigma >= 2.5f)
        {
            q = 0.98711f * sigma - 0.96330f;
        }
        else if ((sigma >= 0.5f) && (sigma < 2.5f))
        {
            q = 3.97156f - 4.14554f * (float)Math.sqrt((double)1 - 0.26891 * sigma);
        }
        else
        {
            q = 0.1147705018520355224609375f;
        }
        q2 = q * q;
        q3 = q * q2;
        c.b[0] = (1.57825f + (2.44413f*q) + (1.4281f *q2) + (0.422205f*q3));
        c.b[1] = ((2.44413f*q) + (2.85619f*q2) + (1.26661f *q3));
        c.b[2] = (-((1.4281f*q2) + (1.26661f *q3)));
        c.b[3] = ((0.422205f*q3));
        c.B = 1.0f - ((c.b[1] + c.b[2] + c.b[3]) / c.b[0]);
        c.sigma = sigma;
        c.N = 3;
    }

    public class mean_var{
        public float mean;
        public float var;
        public mean_var(){
        }
    }

    public void compute_mean_var(float[] src, mean_var mv, int size, int bytes){
        float vsquared;
        int i, j;
        int pos;
        vsquared = 0.0f;
	    mv.mean = 0.0f;

        for (i = 0; i < size; i += bytes)
        {
            for (j = 0; j < 3; j++)
            {
                pos = i + j;
			    mv.mean += src[pos];
                vsquared += src[pos] * src[pos];
            }
        }

	    mv.mean /= (float)size; /* mean */
        vsquared /= (float)size; /* mean (x^2) */
	    mv.var = (vsquared - (mv.mean * mv.mean));
	    mv.var = (float)Math.sqrt(mv.var); /* var */
    }

    public void gausssmooth(float[] in, float[] out, int pos, int size, int rowstride, gauss3_coefs c){
        int i, n, bufsize;
        bufsize = size + 3;
        int Size = size - 1;
        float[] w1 = new float[bufsize];
        float[] w2 = new float[bufsize];
        w1[0] = in[pos];
        w1[1] = in[pos];
        w1[2] = in[pos];
        for (i = 0, n = 3;i <= Size;i++, n++){
            w1[n] = (float) (c.B * in[pos + i * rowstride] +
                    ((c.b[1] * w1[n - 1] +
                            c.b[2] * w1[n - 2] +
                            c.b[3] * w1[n - 3]) / c.b[0]));
        }
        w2[Size + 1] = w1[Size + 3];
        w2[Size + 2] = w1[Size + 3];
        w2[Size + 3] = w1[Size + 3];
        for (i = Size, n = i;i >= 0;i--, n--){
            w2[n] = out[pos + i * rowstride] = (float)(c.B * w1[n] +
                    ((c.b[1] * w2[n + 1] +
                            c.b[2] * w2[n + 2] +
                            c.b[3] * w2[n + 3]) / c.b[0]));
        }

    }

    public void MSRCR(int[] src){
        float[] in = new float[mWidth * mHeight];
        float[] out = new float[mWidth * mHeight];

        retinex_scales_distribution(RetinexScales, rvals.nscales, rvals.scales_mode, rvals.scale);
        float weight = 1.0f / (float)rvals.nscales;
        int channelsize = mHeight * mWidth;
        int size = mHeight * mWidth * 3;
        int scale;
        int i, j, pos;
        int row, col;
        gauss3_coefs coef = new gauss3_coefs();
        float[] dst = new float[size];
        for (j = 0;j < size; j++)
            dst[j] = 0;
        for (int channel = 0; channel < 3; channel ++){
            for (i = 0, pos = channel;i < channelsize; i++, pos+=3){
                in[i] = (float) (src[pos] + 1.0f);
            }
            for (scale = 0;scale < rvals.nscales; scale++){
                compute_coefs3(coef, RetinexScales[scale]);
			/*
			*  Filtering (smoothing) Gaussian recursive.
			*
			*  Filter rows first
			*/
			    /* test
			    System.out.print("channel:" + channel + "\n");
			    System.out.print("COEF\n");
			    System.out.print(coef.B + " " + coef.N + " " + coef.sigma + "\n");
                */
                for (row = 0; row < mHeight; row++)
                {
                    pos = row * mWidth;
                    gausssmooth(in, out, pos, mWidth, 1, coef);
                }

                for (j = 0;j < channelsize;j++){
                    in[j] = out[j];
                    out[j] = 0;
                }

			/*
			*  Filtering (smoothing) Gaussian recursive.
			*
			*  Second columns
			*/
                for (col = 0; col < mWidth; col++)
                {
                    pos = col;
                    gausssmooth(in, out, pos, mHeight, mWidth, coef);
                }

			/*
			Summarize the filtered values.
			In fact one calculates a ratio between the original values and the filtered values.
			*/
                for (i = 0, pos = channel; i < channelsize; i++, pos += 3)
                {
                    dst[pos] += weight * (float)(log(src[pos] + 1.0f) - log(out[i]));
                }
            }
        }

        /*
	Final calculation with original value and cumulated filter values.
	The parameters gain, alpha and offset are constants.
	*/
	/* Ci(x,y)=log[a Ii(x,y)]-log[ Ei=1-s Ii(x,y)] */


        float alpha = 128.0f;
        float gain = 1.0f;
        float offset = 0.0f;


        for (i = 0; i < size; i += 3)
        {
            float logl;

            logl = (float)log((float)src[i] + (float)src[i + 1] + (float)src[i + 2] + 3.0f);

            dst[i] = gain * ((float)(log(alpha * (src[i] + 1.0f)) - logl) * dst[i]) + offset;
            dst[i + 1] = gain * ((float)(log(alpha * (src[i + 1] + 1.0f)) - logl) * dst[i + 1]) + offset;
            dst[i + 2] = gain * ((float)(log(alpha * (src[i + 2] + 1.0f)) - logl) * dst[i + 2]) + offset;
        }

	/*
	Adapt the dynamics of the colors according to the statistics of the first and second order.
	The use of the variance makes it possible to control the degree of saturation of the colors.
	*/
        mean_var mv = new mean_var();
        compute_mean_var(dst, mv, size, 3);
        float mini = mv.mean - rvals.cvar*mv.var;
        float maxi = mv.mean + rvals.cvar*mv.var;
        float range = maxi - mini;

        if (range == 0.0f) range = 1.0f;

        for (i = 0; i < size; i += 3)
        {

            for (j = 0; j < 3; j++)
            {
                pos = i + j;
                float c = 255 * (dst[pos] - mini) / range;
                src[pos] = c < 0 ? 0 : (c > 255 ? 255 : (int)c);
            }
        }
    }

    public Bitmap DehazeHeavy(){
        rvals = new RetinexParams(240, 3, RETINEX_UNIFORM, 2.0f);
        int[] dst = ImageUtils.Bitmap2IntArrayRGB(srcbmp);
        //Log.d("DehazeHeavy","dst length is " + dst.length);
        MSRCR(dst);
        Bitmap result = ImageUtils.IntArrayRGB2Bitmap(dst, mWidth, mHeight);
        return result;
    }

}
