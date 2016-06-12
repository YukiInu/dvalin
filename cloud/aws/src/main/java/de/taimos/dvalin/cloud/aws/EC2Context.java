package de.taimos.dvalin.cloud.aws;

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient;
import com.amazonaws.services.autoscaling.model.AutoScalingGroup;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingGroupsResult;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesRequest;
import com.amazonaws.services.autoscaling.model.DescribeAutoScalingInstancesResult;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.util.EC2MetadataUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Component
public class EC2Context {

    @AWSClient
    private AmazonEC2Client ec2;

    @AWSClient
    private AmazonAutoScalingClient autoScaling;


    /**
     * @return the instance id
     */
    public String getInstanceId() {
        return EC2MetadataUtils.getInstanceId();
    }


    /**
     * <br>
     * Needed AWS actions:
     * <ul>
     * <li>ec2:DescribeInstances</li>
     * </ul>
     *
     * @return the tags of the current instance
     */
    public Map<String, String> getInstanceTags() {
        Map<String, String> map = new HashMap<>();
        Instance instance = this.getInstance();
        for (Tag tag : instance.getTags()) {
            map.put(tag.getKey(), tag.getValue());
        }
        return map;
    }


    /**
     * <br>
     * Needed AWS actions:
     * <ul>
     * <li>autoscaling:DescribeAutoScalingInstances</li>
     * </ul>
     *
     * @return the name of the auto scaling group the current instance is a member of
     */
    public String getAutoScalingGroup() {
        DescribeAutoScalingInstancesRequest req = new DescribeAutoScalingInstancesRequest();
        req.setInstanceIds(Collections.singleton(this.getInstanceId()));
        DescribeAutoScalingInstancesResult result = this.autoScaling.describeAutoScalingInstances(req);
        if (result.getAutoScalingInstances().size() != 1) {
            throw new IllegalStateException("Found multiple auto scaling instances");
        }
        return result.getAutoScalingInstances().get(0).getAutoScalingGroupName();
    }


    /**
     * <br>
     * Needed AWS actions:
     * <ul>
     * <li>autoscaling:DescribeAutoScalingGroups</li>
     * </ul>
     *
     * @param autoScalingGroupName
     *         the name to search for
     * @return the members of the given auto scaling group
     */
    public List<String> getAutoScalingMembers(String autoScalingGroupName) {
        AutoScalingGroup autoScalingGroup = this.getAutoScalingGroup(autoScalingGroupName);

        List<String> list = new ArrayList<>();
        for (com.amazonaws.services.autoscaling.model.Instance instance : autoScalingGroup.getInstances()) {
            if (instance.getHealthStatus().equals("Healthy")) {
                list.add(instance.getInstanceId());
            }
        }
        return list;
    }


    /**
     * <br>
     * Needed AWS actions:
     * <ul>
     * <li>autoscaling:DescribeAutoScalingGroups</li>
     * <li>ec2:DescribeInstances</li>
     * </ul>
     *
     * @param autoScalingGroupName
     *         the name of the group
     * @return the list of private IP addresses of the members
     */
    public List<String> getPrivateAutoScalingMemberIPs(String autoScalingGroupName) {
        List<String> members = this.getAutoScalingMembers(autoScalingGroupName);
        DescribeInstancesRequest req = new DescribeInstancesRequest();
        req.setInstanceIds(members);
        DescribeInstancesResult result = this.ec2.describeInstances(req);
        List<String> list = new ArrayList<>();
        for (Reservation reservation : result.getReservations()) {
            for (Instance instance : reservation.getInstances()) {
                if (instance.getState().getName().equals("running")) {
                    list.add(instance.getPrivateIpAddress());
                }
            }
        }
        return list;
    }


    /**
     * <br>
     * Needed AWS actions:
     * <ul>
     * <li>autoscaling:DescribeAutoScalingGroups</li>
     * </ul>
     *
     * @param autoScalingGroupName
     *         the name to search for
     * @return the given auto scaling group
     */
    public AutoScalingGroup getAutoScalingGroup(String autoScalingGroupName) {
        DescribeAutoScalingGroupsRequest req = new DescribeAutoScalingGroupsRequest();
        req.setAutoScalingGroupNames(Collections.singleton(autoScalingGroupName));
        DescribeAutoScalingGroupsResult result = this.autoScaling.describeAutoScalingGroups(req);
        if (result.getAutoScalingGroups().size() != 1) {
            throw new IllegalStateException("Found multiple auto scaling groups");
        }
        return result.getAutoScalingGroups().get(0);
    }


    /**
     * <br>
     * Needed AWS actions:
     * <ul>
     * <li>ec2:DescribeInstances</li>
     * </ul>
     *
     * @return the current EC2 instance
     */
    public Instance getInstance() {
        DescribeInstancesRequest req = new DescribeInstancesRequest();
        req.setInstanceIds(Collections.singleton(this.getInstanceId()));
        DescribeInstancesResult result = this.ec2.describeInstances(req);
        if (result.getReservations().size() != 1) {
            throw new IllegalStateException("Found multiple instances");
        }
        List<Instance> instances = result.getReservations().get(0).getInstances();
        if (instances.size() != 1) {
            throw new IllegalStateException("Found multiple instances");
        }
        return instances.get(0);
    }

}