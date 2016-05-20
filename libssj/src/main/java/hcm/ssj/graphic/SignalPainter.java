/*
 * SignalPainter.java
 * Copyright (c) 2016
 * Authors: Ionut Damian, Michael Dietz, Frank Gaibler, Daniel Langerenken
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

package hcm.ssj.graphic;

import android.os.Handler;
import android.os.Looper;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;

import hcm.ssj.core.Cons;
import hcm.ssj.core.Consumer;
import hcm.ssj.core.Log;
import hcm.ssj.core.option.Option;
import hcm.ssj.core.option.OptionList;
import hcm.ssj.core.stream.Stream;

/**
 * Created by Johnny on 26.05.2015.
 */
public class SignalPainter extends Consumer
{
    public class Options extends OptionList
    {
        public int colors[] = {0xff0077cc, 0xffff9900, 0xff009999, 0xff990000, 0xffff00ff, 0xff000000, 0xff339900};
        public final Option<Double> size = new Option<>("size", 10., Cons.Type.DOUBLE, "in seconds");
        public final Option<Boolean> legend = new Option<>("legend", true, Cons.Type.BOOL, "");
        public final Option<Boolean> manualBounds = new Option<>("manualBounds", false, Cons.Type.BOOL, "");
        public final Option<Double> min = new Option<>("min", 0., Cons.Type.DOUBLE, "");
        public final Option<Double> max = new Option<>("max", 1., Cons.Type.DOUBLE, "");
        public final Option<Integer> secondScaleDim = new Option<>("secondScaleDim", -1, Cons.Type.INT, "put a dimension on the secondary scale (use -1 to disable)");
        public final Option<Double> secondScaleMin = new Option<>("secondScaleMin", 0., Cons.Type.DOUBLE, "");
        public final Option<Double> secondScaleMax = new Option<>("secondScaleMax", 1., Cons.Type.DOUBLE, "");
        public final Option<Integer> numVLabels = new Option<>("numVLabels", 2, Cons.Type.INT, "");
        public final Option<Integer> numHLabels = new Option<>("numHLabels", 2, Cons.Type.INT, "");
        public final Option<Boolean> renderMax = new Option<>("renderMax", true, Cons.Type.BOOL, "");

        /**
         *
         */
        private Options() {
            add(size);
            add(legend);
            add(manualBounds);
            add(min);
            add(max);
            add(secondScaleDim);
            add(secondScaleMin);
            add(secondScaleMax);
            add(numVLabels);
            add(numHLabels);
            add(renderMax);
        }
    }
    public final Options options = new Options();

    private ArrayList<LineGraphSeries<DataPoint>> _series = new ArrayList<>();

    GraphView _view = null;
    int _maxPoints;

    public SignalPainter()
    {
        _name = "SSJ_consumer_SignalPainter";
    }

    @Override
    public void enter(Stream[] stream_in)
    {
        if(_view == null)
        {
            Log.e("graph view not registered");
            return;
        }

        _view.getViewport().setXAxisBoundsManual(true);
        _view.getViewport().setMinX(0);
        _view.getViewport().setMaxX(options.size.getValue());

        _view.getViewport().setYAxisBoundsManual(options.manualBounds.getValue());
        _view.getViewport().setMaxY(options.max.getValue());
        _view.getViewport().setMinY(options.min.getValue());

        _view.getGridLabelRenderer().setNumHorizontalLabels(options.numHLabels.getValue());
        _view.getGridLabelRenderer().setNumVerticalLabels(options.numVLabels.getValue());

        _view.getLegendRenderer().setVisible(options.legend.getValue());
        _view.getLegendRenderer().setFixedPosition(10, 10);

        _view.getGridLabelRenderer().setLabelVerticalWidth(100);

        if(!options.renderMax.getValue())
            _maxPoints = (int)(options.size.getValue() * stream_in[0].sr) +1;
        else
            _maxPoints = (int)(options.size.getValue() * (stream_in[0].sr / (double)stream_in[0].num)) +1;

        createSeries(_view, stream_in[0]);
    }

    @Override
    protected void consume(Stream[] stream_in)
    {
        switch(stream_in[0].type)
        {
            case CHAR:
            {
                char[] in = stream_in[0].ptrC();

                for (int i = 0; i < stream_in[0].dim; i++)
                {
                    char max = Character.MIN_VALUE;
                    char value;
                    double time = stream_in[0].time;
                    for (int j = 0; j < stream_in[0].num; j++, time += stream_in[0].step)
                    {
                        value = in[j * stream_in[0].dim + i];

                        if(!options.renderMax.getValue())
                        {
                            pushData(i, value, time);
                        }
                        else if (value > max)
                            max = value;
                    }

                    if(options.renderMax.getValue())
                        pushData(i, max, stream_in[0].time);
                }
                break;
            }

            case SHORT:
            {
                short[] in = stream_in[0].ptrS();

                for (int i = 0; i < stream_in[0].dim; i++)
                {
                    short max = Short.MIN_VALUE;
                    short value;
                    double time = stream_in[0].time;
                    for (int j = 0; j < stream_in[0].num; j++, time += stream_in[0].step)
                    {
                        value = in[j * stream_in[0].dim + i];

                        if(!options.renderMax.getValue())
                            pushData(i, value, time);
                        else if (value > max)
                            max = value;
                    }

                    if(options.renderMax.getValue())
                        pushData(i, max, stream_in[0].time);
                }
                break;
            }

            case INT:
            {
                int[] in = stream_in[0].ptrI();

                for (int i = 0; i < stream_in[0].dim; i++)
                {
                    int max = Integer.MIN_VALUE;
                    int value;
                    double time = stream_in[0].time;
                    for (int j = 0; j < stream_in[0].num; j++, time += stream_in[0].step)
                    {
                        value = in[j * stream_in[0].dim + i];

                        if(!options.renderMax.getValue())
                            pushData(i, value, time);
                        else if (value > max)
                            max = value;
                    }

                    if(options.renderMax.getValue())
                        pushData(i, max, stream_in[0].time);
                }
                break;
            }

            case LONG:
            {
                long[] in = stream_in[0].ptrL();

                for (int i = 0; i < stream_in[0].dim; i++)
                {
                    long max = Long.MIN_VALUE;
                    long value;
                    double time = stream_in[0].time;
                    for (int j = 0; j < stream_in[0].num; j++, time += stream_in[0].step)
                    {
                        value = in[j * stream_in[0].dim + i];

                        if(!options.renderMax.getValue())
                            pushData(i, value, time);
                        else if (value > max)
                            max = value;
                    }

                    if(options.renderMax.getValue())
                        pushData(i, max, stream_in[0].time);
                }
                break;
            }

            case FLOAT:
            {
                float[] in = stream_in[0].ptrF();

                for (int i = 0; i < stream_in[0].dim; i++)
                {
                    float max = -1 * Float.MAX_VALUE;
                    float value;
                    double time = stream_in[0].time;
                    for (int j = 0; j < stream_in[0].num; j++, time += stream_in[0].step)
                    {
                        value = in[j * stream_in[0].dim + i];

                        if(!options.renderMax.getValue())
                            pushData(i, value, time);
                        else if (value > max)
                            max = value;
                    }

                    if(options.renderMax.getValue())
                        pushData(i, max, stream_in[0].time);
                }
                break;
            }

            case DOUBLE:
            {
                double[] in = stream_in[0].ptrD();

                for (int i = 0; i < stream_in[0].dim; i++)
                {
                    double max = -1 * Double.MAX_VALUE;
                    double value;
                    double time = stream_in[0].time;
                    for (int j = 0; j < stream_in[0].num; j++, time += stream_in[0].step)
                    {
                        value = in[j * stream_in[0].dim + i];

                        if(!options.renderMax.getValue())
                            pushData(i, value, time);
                        else if (value > max)
                            max = value;
                    }

                    if(options.renderMax.getValue())
                        pushData(i, max, stream_in[0].time);
                }
                break;
            }

            default:
                Log.w("unsupported data type");
                return;
        }

    }

    @Override
    public void flush(Stream[] stream_in) {

        _series.clear();

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            public void run() {
                _view.removeAllSeries();
                _view.clearSecondScale();
            }
        }, 1);
    }

    private void pushData(final int dim, double value, double time)
    {
        //apparently GraphView can't render infinity
        if(Double.isNaN(value) || Double.isInfinite(value) || value == -1 * Double.MAX_VALUE  || value == Double.MAX_VALUE)
            return;

        final DataPoint p = new DataPoint(time, value);

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable()
        {
            public void run()
            {
                if(dim < _series.size())
                    _series.get(dim).appendData(p, true, _maxPoints);
            }
        }, 1);
    }

    private void createSeries(final GraphView view, final Stream stream)
    {
        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable()
        {
            public void run()
            {
                for(int i=0; i < stream.dim; i++)
                {
                    LineGraphSeries<DataPoint> s = new LineGraphSeries<>();
                    s.setTitle(stream.dataclass[i]);
                    s.setColor(options.colors[i % options.colors.length]);

                    _series.add(s);

                    if(options.secondScaleDim.getValue() == i)
                    {
                        view.getSecondScale().setMinY(options.secondScaleMin.getValue());
                        view.getSecondScale().setMaxY(options.secondScaleMax.getValue());
                        view.getSecondScale().addSeries(s);
                    }
                    else
                    {
                        _view.addSeries(s);
                    }
                }
            }
        }, 1);
    }

    public void registerGraphView(GraphView view)
    {
        _view = view;
    }
}
