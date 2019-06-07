/*
 *
 *          Copyright (c) 2013,2019  AT&T Knowledge Ventures
 *                     SPDX-License-Identifier: MIT
 */
package com.hibiup.xacml;

import com.att.research.xacml.util.XACMLProperties;
import com.att.research.xacmlatt.pdp.std.StdPolicyFinderFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class PolicyConfiguration {
	private List<File> rootPolicies			= new ArrayList<>();
	private List<File> referencedPolicies	= new ArrayList<>();
	
	private void setXACMLProperty(String propertyName, List<File> listFiles) {
		Iterator<File> iterFiles			= listFiles.iterator();
		StringBuilder stringBuilderIdList	= new StringBuilder();
		while (iterFiles.hasNext()) {
			File file	= iterFiles.next();
			if (stringBuilderIdList.length() > 0) {
				stringBuilderIdList.append(',');
			}
			stringBuilderIdList.append(file.getName());
			
			XACMLProperties.setProperty(file.getName() + StdPolicyFinderFactory.PROP_FILE, file.getAbsolutePath());
		}
		XACMLProperties.setProperty(propertyName, stringBuilderIdList.toString());
	}

	public void setXACMLProperties() {
		if (this.rootPolicies.size() > 0) {
			this.setXACMLProperty(XACMLProperties.PROP_ROOTPOLICIES, this.rootPolicies);
		}
		if (this.referencedPolicies.size() > 0) {
			this.setXACMLProperty(XACMLProperties.PROP_REFERENCEDPOLICIES, this.referencedPolicies);
		}
	}
	
	private void loadProperty(File fileDir, Properties properties, String propertyName, List<File> listFiles) {
		String fileNameList	= properties.getProperty(propertyName);
		if (fileNameList != null) {
			String[] fileNameArray = fileNameList.split("[,]",0);
			if (fileNameArray.length > 0) {
				for (String fileName : fileNameArray) {
					File file	= new File(fileDir, fileName);
					if (file.exists() && file.canRead()) {
						listFiles.add(file);
					}
				}
			}
		}
	}

	void loadPolicies(File fileRepository) throws IOException {
		Properties propertiesRepository	= new Properties();
		try (InputStream is = new FileInputStream(fileRepository)) {
			propertiesRepository.load(is);
		}
		this.loadProperty(fileRepository.getParentFile(), propertiesRepository, XACMLProperties.PROP_ROOTPOLICIES, this.rootPolicies);
		this.loadProperty(fileRepository.getParentFile(), propertiesRepository, XACMLProperties.PROP_REFERENCEDPOLICIES, this.referencedPolicies);
	}
	
	void addPolicy(File filePolicy) {
		this.rootPolicies.add(filePolicy);
	}
}
