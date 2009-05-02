/**
 * Copyright 2008 Michael Ogawa
 *
 * This file is part of code_swarm.
 *
 * code_swarm is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * code_swarm is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with code_swarm.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @brief Definition of the colored histogram elements
 */
class ColorBins
{
  // The color is the key, and the count is the value
  Map<Integer, Integer> colorMap;
  int totalCount;
  int [] keys;
  int keyCount;

  ColorBins()
  {
    colorMap = new HashMap<Integer, Integer>();
    totalCount = 0;
    keyCount = 0;
  }

  public void add( int c )
  {
    if (colorMap.containsKey(c)) {
       int v = colorMap.get(c);
       colorMap.put(c, v + 1);
    }
    else {
       colorMap.put(c, 1);
    }

    totalCount++;
  }



  public void sort()
  {
    keyCount = colorMap.size();
    keys = new int[keyCount];
    int i = 0;
    for (Integer k : colorMap.keySet()) {
       keys[i++] = k;
    }

    Arrays.sort(keys);
  }
}

