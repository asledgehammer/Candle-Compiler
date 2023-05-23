package com.asledgehammer.candle;

import java.util.Comparator;
import java.util.List;

public class CandleExecutableComparator implements Comparator<CandleExecutable> {

  static CandleExecutableComparator INSTANCE = new CandleExecutableComparator();

  @Override
  public int compare(CandleExecutable a, CandleExecutable b) {
    List<CandleParameter> ap = a.getParameters();
    List<CandleParameter> bp = b.getParameters();
    int aps = ap.size();
    int bps = bp.size();
    int count = aps - bps;
    if (count == 0) {
      if (aps == 0) return 0;

      for (int index = 0; index < aps; index++) {
        CandleParameter apn = ap.get(index);
        CandleParameter bpn = bp.get(index);
        int compare =
            apn.getJavaParameter()
                .getType()
                .getName()
                .compareTo(bpn.getJavaParameter().getType().getName());
        if (compare != 0) return compare;
      }
    }

    return count;
  }
}
