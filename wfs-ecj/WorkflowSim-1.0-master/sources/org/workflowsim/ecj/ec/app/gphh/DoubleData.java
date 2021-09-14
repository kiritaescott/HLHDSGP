/*
  Copyright 2006 by Sean Luke
  Licensed under the Academic Free License version 3.0
  See the file "LICENSE" for more information
*/


package ec.app.gphh;
import ec.gp.*;

public class DoubleData extends GPData
    {
    public double x;    // return value

    public void copyTo(final GPData gpd)   // copy my stuff to another DoubleData
        { ((ec.app.bcdsgp.DoubleData)gpd).x = x; }
    }


