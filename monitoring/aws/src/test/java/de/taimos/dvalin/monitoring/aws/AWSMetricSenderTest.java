package de.taimos.dvalin.monitoring.aws;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.amazonaws.services.cloudwatch.AmazonCloudWatch;
import com.amazonaws.services.cloudwatch.model.Dimension;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;

import de.taimos.dvalin.monitoring.MetricInfo;
import de.taimos.dvalin.monitoring.MetricUnit;

@RunWith(MockitoJUnitRunner.class)
public class AWSMetricSenderTest {

    @Mock
    private AmazonCloudWatch cloudWatch;

    private AWSMetricSender sender;

    @Before
    public void setUp() throws Exception {
        this.sender = new AWSMetricSender();

        Field field = AWSMetricSender.class.getDeclaredField("cloudWatch");
        field.setAccessible(true);
        field.set(this.sender, this.cloudWatch);
    }

    @Test
    public void sendMetric() throws Exception {
        final String ns = "My/Namespace";
        final String metric = "Test metric";
        final MetricUnit unit = MetricUnit.Count;
        MetricInfo info = new MetricInfo(ns, metric, unit);

        final int val = (int) (Math.random() * 100);

        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                PutMetricDataRequest req = (PutMetricDataRequest) invocationOnMock.getArguments()[0];
                Assert.assertEquals(ns, req.getNamespace());
                List<MetricDatum> data = req.getMetricData();
                Assert.assertEquals(1, data.size());
                MetricDatum datum = data.get(0);
                Assert.assertEquals(metric, datum.getMetricName());
                Assert.assertEquals(unit.toString(), datum.getUnit());
                Assert.assertEquals(Double.valueOf(val), datum.getValue());
                return null;
            }
        }).when(this.cloudWatch).putMetricData(Mockito.any(PutMetricDataRequest.class));
    
        this.sender.sendMetric(info, (double) val);
    }

    @Test
    public void sendMetricWithDimension() throws Exception {
        final String ns = "My/Namespace";
        final String metric = "Test metric";
        final MetricUnit unit = MetricUnit.Count;
        final String dimensionName = "id";
        final String dimensionValue = "someId";
        MetricInfo info = new MetricInfo(ns, metric, unit);
        info.withDimension(dimensionName, dimensionValue);

        final int val = (int) (Math.random() * 100);

        Mockito.doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                PutMetricDataRequest req = (PutMetricDataRequest) invocationOnMock.getArguments()[0];
                Assert.assertEquals(ns, req.getNamespace());
                List<MetricDatum> data = req.getMetricData();
                Assert.assertEquals(1, data.size());
                MetricDatum datum = data.get(0);
                Assert.assertEquals(metric, datum.getMetricName());
                Assert.assertEquals(unit.toString(), datum.getUnit());
                Assert.assertEquals(Double.valueOf(val), datum.getValue());

                Assert.assertEquals(1, datum.getDimensions().size());
                Dimension dimension = datum.getDimensions().get(0);
                Assert.assertEquals(dimensionName, dimension.getName());
                Assert.assertEquals(dimensionValue, dimension.getValue());
                return null;
            }
        }).when(this.cloudWatch).putMetricData(Mockito.any(PutMetricDataRequest.class));
    
        this.sender.sendMetric(info, (double) val);
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingNamespace() throws Exception {
        new MetricInfo(null, "someMetric", MetricUnit.Count);
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingMetric() throws Exception {
        new MetricInfo("Some/Namespace", null, MetricUnit.Count);
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingUnit() throws Exception {
        new MetricInfo("Some/Namespace", "someMetric", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyNamespace() throws Exception {
        new MetricInfo("", "someMetric", MetricUnit.Count);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyMetric() throws Exception {
        new MetricInfo("Some/Namespace", "", MetricUnit.Count);
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingDimensionName() throws Exception {
        new MetricInfo("Some/Namespace", "someMetric", MetricUnit.Count).withDimension(null, "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyDimensionName() throws Exception {
        new MetricInfo("Some/Namespace", "someMetric", MetricUnit.Count).withDimension("", "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void missingDimensionValue() throws Exception {
        new MetricInfo("Some/Namespace", "someMetric", MetricUnit.Count).withDimension("name", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void emptyDimensionValue() throws Exception {
        new MetricInfo("Some/Namespace", "someMetric", MetricUnit.Count).withDimension("name", "");
    }

}