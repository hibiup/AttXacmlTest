/*
 *
 *          Copyright (c) 2013,2019  AT&T Knowledge Ventures
 *                     SPDX-License-Identifier: MIT
 */
package com.hibiup.xacml;

import com.att.research.xacml.api.Attribute;
import com.att.research.xacml.api.AttributeValue;
import com.att.research.xacml.api.pdp.ScopeQualifier;
import com.att.research.xacml.api.pdp.ScopeResolver;
import com.att.research.xacml.api.pdp.ScopeResolverResult;
import com.att.research.xacml.std.StdMutableAttribute;
import com.att.research.xacml.std.StdScopeResolverResult;
import com.att.research.xacml.std.StdStatus;
import com.att.research.xacml.std.StdStatusCode;
import com.att.research.xacml.std.datatypes.DataTypes;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.net.URI;
import java.util.*;

public class XACMLScopeResolver implements ScopeResolver {
	private Log logger = LogFactory.getLog(XACMLScopeResolver.class);
	private Map<URI, List<URI>> mapIdentifierToChildren	= new HashMap<>();
	
	public XACMLScopeResolver() {
	}
	
	public void add(URI identifierRoot, URI identifierChild) {
		List<URI> listChildrenRoot = this.mapIdentifierToChildren.computeIfAbsent(identifierRoot, k -> new ArrayList<URI>());
		listChildrenRoot.add(identifierChild);
	}
	
	private void addChildren(Attribute attributeResourceId, URI urnResourceIdValue, boolean bDescendants, List<Attribute> listAttributes) {
		List<URI> listChildren	= this.mapIdentifierToChildren.get(urnResourceIdValue);
		if (listChildren != null) {
			for (URI uriChild : listChildren) {
				AttributeValue<URI> attributeValueURI	= null;
				try {
					attributeValueURI	= DataTypes.DT_ANYURI.createAttributeValue(uriChild);
					if (attributeValueURI != null) {
						listAttributes.add(new StdMutableAttribute(attributeResourceId.getCategory(), attributeResourceId.getAttributeId(), attributeValueURI, attributeResourceId.getIssuer(), attributeResourceId.getIncludeInResults()));
					}
				} catch (Exception ex) {
					this.logger.error("Exception converting URI to an AttributeValue");
				}
				if (bDescendants) {
					this.addChildren(attributeResourceId, uriChild, true, listAttributes);
				}
			}
		}
	}
	
	private void addChildren(Attribute attributeResourceId, boolean bDescendants, List<Attribute> listAttributes) {
		Iterator<AttributeValue<URI>> iterAttributeValueURNs	= attributeResourceId.findValues(DataTypes.DT_ANYURI);
		if (iterAttributeValueURNs != null) {
			while (iterAttributeValueURNs.hasNext()) {
				this.addChildren(attributeResourceId, iterAttributeValueURNs.next().getValue(), bDescendants, listAttributes);
			}
		}
	}
	
	@Override
	public ScopeResolverResult resolveScope(Attribute attributeResourceId, ScopeQualifier scopeQualifier) {
		List<Attribute> listAttributes	= new ArrayList<Attribute>();
		switch(scopeQualifier) {
		case CHILDREN:
			listAttributes.add(attributeResourceId);
			this.addChildren(attributeResourceId, false, listAttributes);
			break;
		case DESCENDANTS:
			listAttributes.add(attributeResourceId);
			this.addChildren(attributeResourceId, true, listAttributes);
			break;
		case IMMEDIATE:
			listAttributes.add(attributeResourceId);
			break;
		default:
			this.logger.error("Unknown ScopeQualifier: " + scopeQualifier.name());
			return new StdScopeResolverResult(new StdStatus(StdStatusCode.STATUS_CODE_SYNTAX_ERROR, "Unknown ScopeQualifier " + scopeQualifier.name()));
		}

		return new StdScopeResolverResult(listAttributes);
	}
}
