package com.jinghu.cad.analysis.utils;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import java.net.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 获取IP方法
 */
@Slf4j
public class IpUtils {
    public static String getIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        log.debug("X-Forwarded-For getIp: {}", ipAddress);
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("Proxy-Client-IP");
            log.debug("Proxy-Client-IP getIp: {}", ipAddress);
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("WL-Proxy-Client-IP");
            log.debug("WL-Proxy-Client-IP getIp: {}", ipAddress);
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED_FOR");
            log.debug("HTTP_X_FORWARDED_FOR getIp: {}", ipAddress);
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_FORWARDED");
            log.debug("HTTP_X_FORWARDED getIp: {}", ipAddress);
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_X_CLUSTER_CLIENT_IP");
            log.debug("HTTP_X_CLUSTER_CLIENT_IP getIp: {}", ipAddress);
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_CLIENT_IP");
            log.debug("HTTP_CLIENT_IP getIp: {}", ipAddress);
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_FORWARDED_FOR");
            log.debug("HTTP_FORWARDED_FOR getIp: {}", ipAddress);
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("HTTP_FORWARDED");
            log.debug("HTTP_FORWARDED getIp: {}", ipAddress);
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getHeader("REMOTE_ADDR");
            log.debug("REMOTE_ADDR getIp: {}", ipAddress);
        }
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
            log.debug("REMOTE_ADDR getIp: {}", ipAddress);
        }
        // 如果是多级代理，则取第一个非unknown的IP地址
        if (ipAddress != null && !ipAddress.isEmpty() && ipAddress.contains(",")) {
            ipAddress = ipAddress.split(",")[0].trim();
        }
        if ("0:0:0:0:0:0:0:1".equals(ipAddress)) {
            ipAddress = "127.0.0.1";
        }
        return ipAddress;
    }


    public static List<String> getHostIp() {
        return getHostIp(null, "ipv4");
    }

    public static List<String> getHostIp(String netName) {
        return getHostIp(netName, "ipv4");
    }

    public static List<String> getHostIp(String netName, String ipVersion) {
        if (!"ipv4".equalsIgnoreCase(ipVersion) && !"ipv6".equalsIgnoreCase(ipVersion)) {
            throw new IllegalArgumentException("Invalid IP version: " + ipVersion);
        }
        try {
            return Collections.list(NetworkInterface.getNetworkInterfaces()).stream()
                    .filter(IpUtils::isValidNetworkInterface) // 过滤有效网卡
                    .filter(net -> netName == null || netName.trim().isEmpty() || netName.equals(net.getName()))
                    .flatMap(net -> getIpAddresses(net, ipVersion).stream())
                    .collect(Collectors.toList());
        } catch (SocketException e) {
            log.error("Failed to get network interfaces: " + e.getMessage());
            return Collections.emptyList();
        }
    }

    private static List<String> getIpAddresses(NetworkInterface networkInterface, String ipVersion) {
        return Collections.list(networkInterface.getInetAddresses()).stream()
                .filter(inetAddress -> ("ipv4".equalsIgnoreCase(ipVersion) && inetAddress instanceof Inet4Address) ||
                        ("ipv6".equalsIgnoreCase(ipVersion) && inetAddress instanceof Inet6Address))
                .map(InetAddress::getHostAddress)
                .collect(Collectors.toList());
    }

    private static boolean isValidNetworkInterface(NetworkInterface net) {
        try {
            return !net.isLoopback() // 排除回环网卡 (127.0.0.1)
                    && !net.isVirtual() // 确保网卡已启用
                    && net.isUp()
                    && !net.getName().startsWith("docker")  // linux 排除 Docker 网卡
                    && !net.getName().startsWith("br-")     // linux 排除 Docker 桥接网卡
                    && !net.getName().startsWith("veth")    // linux 排除 Docker 容器网卡
                    && !net.getDisplayName().contains("Hyper-V"); // win 排除 Hyper-V 相关网卡
        } catch (SocketException e) {
            return false;
        }
    }

    @SneakyThrows
    public static boolean isInRange(String ipAddress, String subnetCIDR) {
        InetAddress address = InetAddress.getByName(ipAddress);
        if ("0.0.0.0".equals(subnetCIDR)) {
            subnetCIDR = "0.0.0.0/0";
        }
        if (!subnetCIDR.contains("/")) {
            subnetCIDR = subnetCIDR + "/32";
        }
        InetAddress subnetAddress = InetAddress.getByName(subnetCIDR.substring(0, subnetCIDR.indexOf('/')));
        int subnetPrefixLength = Integer.parseInt(subnetCIDR.substring(subnetCIDR.indexOf('/') + 1));
        return isInRange(address, subnetAddress, subnetPrefixLength);
    }

    private static boolean isInRange(InetAddress address, InetAddress subnetAddress, int subnetPrefixLength) {
        byte[] addressBytes = address.getAddress();
        byte[] subnetBytes = subnetAddress.getAddress();

        if (addressBytes.length != subnetBytes.length) {
            return false;
        }

        int numFullBytes = subnetPrefixLength / 8;
        int bitsInPartialByte = subnetPrefixLength % 8;

        for (int i = 0; i < numFullBytes; i++) {
            if (addressBytes[i] != subnetBytes[i]) {
                return false;
            }
        }

        if (bitsInPartialByte > 0) {
            int partialByteMask = (0xFF << (8 - bitsInPartialByte)) & 0xFF;
            return (addressBytes[numFullBytes] & partialByteMask) == (subnetBytes[numFullBytes] & partialByteMask);
        }

        return true;
    }


}