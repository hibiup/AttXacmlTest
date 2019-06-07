/*
 *
 *          Copyright (c) 2013,2019  AT&T Knowledge Ventures
 *                     SPDX-License-Identifier: MIT
 */
package com.hibiup.xacml;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

public class PolicyRepository {
	private static final Log logger = LogFactory.getLog(PolicyRepository.class);

	private final Map<String, PolicyConfiguration> mapPolicies = new HashMap<>();

	public PolicyConfiguration getPolicy(String name) {
		return this.mapPolicies.get(name);
	}

	private static String getPolicyName(String fileName, int itemPos) {
		return (itemPos == 0 ? "NULL" : fileName.substring(0, itemPos));
	}
	
	private static String getPolicyName(File file) {
		String fileName	= file.getName();
		int itemPos		= fileName.indexOf("Policy");
		if (itemPos >= 0) {
			return getPolicyName(fileName, itemPos);
		} else if ((itemPos = fileName.indexOf("Repository")) >= 0) {
			return getPolicyName(fileName, itemPos);
		} else {
			return null;
		}
	}
	
	public PolicyRepository(File fileDir) throws IOException {
		Files.walkFileTree(fileDir.toPath(), new FileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
				logger.info("Scanning directory " + dir.getFileName());
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				File fileVisited	= file.toFile();
				String fileName		= fileVisited.getName();
				if (fileName.endsWith(".xml") || fileName.endsWith(".properties")) {
					String policyName	= getPolicyName(fileVisited);
					if (policyName != null) {
						PolicyConfiguration policyConfig = mapPolicies.get(policyName);
						if (policyConfig == null) {
							logger.info("Added test " + policyName);
							policyConfig = new PolicyConfiguration();
							mapPolicies.put(policyName, policyConfig);
						}
						if (fileName.endsWith("Policy.xml")) {
							policyConfig.addPolicy(fileVisited);
						} else if (fileName.endsWith("Repository.properties")) {
							policyConfig.loadPolicies(fileVisited);
						}
					}
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFileFailed(Path file, IOException exc) {
				logger.warn("Skipped " + file.getFileName());
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
