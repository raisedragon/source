import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

import org.junit.Test;

public class a
{
	@Test
	public void test()
	{
		for (int i = 0; i < 100; i++)
		{
			BigDecimal packingVolume = new BigDecimal(1000.50+0.001*i);
			System.out.print(new DecimalFormat("0.0000").format(packingVolume));
			packingVolume = BigDecimal.valueOf((Math.ceil(packingVolume.doubleValue()*100)/100));
			System.out.println("==>" +new DecimalFormat("0.00").format(packingVolume));
//			System.out.print(new DecimalFormat("0.0000").format(packingVolume));
//			packingVolume = packingVolume.multiply(BigDecimal.valueOf(100)).add(new BigDecimal(0.5)).setScale(0, RoundingMode.DOWN).divide(BigDecimal.valueOf(100));
//			System.out.println(" ==> "+new DecimalFormat("0.0000").format(packingVolume));
		}
	}
}
