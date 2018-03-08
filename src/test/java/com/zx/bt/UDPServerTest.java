package com.zx.bt;

import com.sun.javafx.binding.StringFormatter;
import com.zx.bt.entity.InfoHash;
import com.zx.bt.repository.InfoHashRepository;
import com.zx.bt.util.Bencode;
import com.zx.bt.util.CodeUtil;
import com.zx.bt.util.HttpClientUtil;
import io.netty.util.CharsetUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.RandomStringGenerator;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * author:ZhengXing
 * datetime:2018-02-13 14:04
 * 测试UDP
 */
@Slf4j
public class UDPServerTest extends BtApplicationTests{

	@Autowired
	private InfoHashRepository infoHashRepository;




}