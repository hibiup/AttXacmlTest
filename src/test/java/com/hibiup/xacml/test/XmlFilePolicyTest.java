package com.hibiup.xacml.test;

import com.att.research.xacml.api.*;
import com.att.research.xacml.api.pdp.*;
import com.att.research.xacml.std.*;
import com.att.research.xacml.std.dom.DOMRequest;
import com.att.research.xacml.std.dom.DOMStructureException;
import com.att.research.xacml.util.FactoryException;
import com.hibiup.xacml.PolicyRepository;
import com.hibiup.xacml.XACMLScopeResolver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RunWith(JUnit4.class)
public class XmlFilePolicyTest {
    private static final Log logger	= LogFactory.getLog(XmlFilePolicyTest.class);

    public XmlFilePolicyTest() throws IOException {
    }

    private XACMLScopeResolver buildScopeResolver() {
        XACMLScopeResolver scopeResolver = new XACMLScopeResolver();

        try {
                URI ID_SCOPE_ROOT	= new URI("urn:root");
                URI ID_SCOPE_CHILD1	= new URI("urn:root:child1");
                URI ID_SCOPE_CHILD2	= new URI("urn:root:child2");
                URI ID_SCOPE_C1D1	= new URI("urn:root:child1:descendant1");
                URI ID_SCOPE_C1D2	= new URI("urn:root:child1:descendant2");
                URI ID_SCOPE_C2D1	= new URI("urn:root:child2:descendant1");
                URI ID_SCOPE_C2D2	= new URI("urn:root:child2:descendant2");

                scopeResolver.add(ID_SCOPE_ROOT, ID_SCOPE_CHILD1);
                scopeResolver.add(ID_SCOPE_CHILD1, ID_SCOPE_C1D1);
                scopeResolver.add(ID_SCOPE_CHILD1, ID_SCOPE_C1D2);
                scopeResolver.add(ID_SCOPE_ROOT, ID_SCOPE_CHILD2);
                scopeResolver.add(ID_SCOPE_CHILD2, ID_SCOPE_C2D1);
                scopeResolver.add(ID_SCOPE_CHILD2, ID_SCOPE_C2D2);
        } catch (Exception ex) {
            logger.error(ex.getMessage(), ex);
        }

        return scopeResolver;
    }

    private final String pdpRepositoryLocation = "src/test/resources";

    private PolicyRepository policyRepository = new PolicyRepository(new File(pdpRepositoryLocation + "/policies"));
    private XACMLScopeResolver scopeResolver = buildScopeResolver();

    protected void printRequest(Request request){
        System.out.println(request.getStatus());                   // null
        System.out.println(request.getRequestDefaults());          // null
        System.out.println(request.getReturnPolicyIdList());       // false
        System.out.println(request.getCombinedDecision());         // false
        request.getRequestAttributes().forEach(attr -> {           // 4 items
            System.out.println(attr.getCategory());                // urn:oasis:names:tc:xacml:1.0:subject-category:access-subject
            attr.getAttributes().forEach( att -> {
                System.out.println(att.getCategory());             // urn:oasis:names:tc:xacml:1.0:subject-category:access-subject
                System.out.println(att.getAttributeId());          // urn:oasis:names:tc:xacml:1.0:subject:subject-id
                att.getValues().forEach(value -> {
                    System.out.println(value.getDataTypeId());     // http://www.w3.org/2001/XMLSchema#string
                    System.out.println(value.getValue());          // Julius Hibbert
                    System.out.println(value.getXPathCategory());  // null
                });
                System.out.println(att.getIssuer());               // null
                System.out.println(att.getIncludeInResults());     // false
            });
            System.out.println(attr.getContentRoot());          // null
            System.out.println(attr.getXmlId());                // null

        });
        System.out.println(request.getMultiRequests());         // 0 items
    }

    @Test
    public synchronized void testLocalPdpEngine() throws FactoryException, DOMStructureException, PDPException, URISyntaxException {
        String testPolicy = "IIA001";

        policyRepository.getPolicy(testPolicy).setXACMLProperties();
        PDPEngineFactory pdpEngineFactory	= PDPEngineFactory.newInstance();
        //pdpEngineFactory.setScopeResolver(scopeResolver);
        PDPEngine pdpEngine		= pdpEngineFactory.newEngine();
        Request request = DOMRequest.load(new File(pdpRepositoryLocation + "/requests/" +testPolicy + "Request.xml"));

        Response response	= pdpEngine.decide(request);
        response.getResults().forEach(f -> {
            String decision = f.getDecision().name();
            logger.info(decision);
            switch(testPolicy) {
                case "IIA001": {assert(decision=="PERMIT"); break;}          // Matched
                case "IIA002": {assert(decision=="NOTAPPLICABLE"); break;}   // Not Matched
                case "IIA004": {assert(decision=="INDETERMINATE"); break;}   // Invalid syntax
                case "IIA007": {assert(decision=="INDETERMINATE"); break;}   // Must be resent
                case "IIA010": {assert(decision=="PERMIT"); break;}   // Matches age(45:Integer)
            }
        });
    }

    @Test
    public synchronized void testStdRequest() throws FactoryException, DOMStructureException, PDPException, URISyntaxException {
        String testPolicy = "IIA001";

        policyRepository.getPolicy(testPolicy).setXACMLProperties();
        PDPEngineFactory pdpEngineFactory	= PDPEngineFactory.newInstance();
        //pdpEngineFactory.setScopeResolver(scopeResolver);
        PDPEngine pdpEngine		= pdpEngineFactory.newEngine();
        Request request = (generateRequest());

        Response response	= pdpEngine.decide(request);
        response.getResults().forEach(f -> {
            String decision = f.getDecision().name();
            logger.info(decision);
            switch(testPolicy) {
                case "IIA001": {assert(decision=="PERMIT"); break;}          // Matched
                case "IIA002": {assert(decision=="NOTAPPLICABLE"); break;}   // Not Matched
                case "IIA004": {assert(decision=="INDETERMINATE"); break;}   // Invalid syntax
                case "IIA007": {assert(decision=="INDETERMINATE"); break;}   // Must be resent
                case "IIA010": {assert(decision=="PERMIT"); break;}   // Matches age(45:Integer)
            }
        });
    }


    public synchronized Request generateRequest() throws URISyntaxException {
        Status status = null;    // StdStatus
        RequestDefaults requestDefaults = null;  // StdRequestDefaults

        RequestAttributes requestAttr1 = generateRequestAttributes(
                XACML3.ID_SUBJECT_CATEGORY_ACCESS_SUBJECT,
                XACML1.ID_SUBJECT_SUBJECT_ID,    // "urn:oasis:names:tc:xacml:1.0:subject:subject-id",
                XACML.ID_DATATYPE_STRING,
                "Julius Hibbert"
        );
        RequestAttributes requestAttr2 = generateRequestAttributes(
                XACML3.ID_ATTRIBUTE_CATEGORY_RESOURCE,
                XACML1.ID_RESOURCE_RESOURCE_ID,  //"urn:oasis:names:tc:xacml:1.0:resource:resource-id",
                XACML.ID_DATATYPE_ANYURI,
                "http://medico.com/record/patient/BartSimpson"
        );
        RequestAttributes requestAttr3 = generateRequestAttributes(
                XACML3.ID_ATTRIBUTE_CATEGORY_ACTION,
                XACML1.ID_ACTION_ACTION_ID,      // "urn:oasis:names:tc:xacml:1.0:action:action-id",
                XACML.ID_DATATYPE_STRING,
                "read"
        );
        RequestAttributes requestAttr4 = generateRequestAttributes(
                XACML3.ID_ATTRIBUTE_CATEGORY_ENVIRONMENT,
                null,
                null,
                null
        );

        Collection<RequestReference> references = null;
        StdRequest stdReq = new StdRequest(
                status,                     // null
                requestDefaults,            // null
                false,       // false
                false,      // false
                Arrays.asList(requestAttr1, requestAttr2, requestAttr3, requestAttr4),  // 4 items
                references                  // 0 items
        );

        return stdReq;
    }


    protected <T> RequestAttributes generateRequestAttributes(Identifier category,
                                                              Identifier attributeId,
                                                              Identifier dateType,
                                                              T value) throws URISyntaxException {
        // AttributeValue
        AttributeValue attributeValue = (null != value)?new StdAttributeValue<>(
                dateType,
                value):null;

        // Attribute
        Attribute attr = (null == attributeId) ? null : new StdAttribute(
                category,
                attributeId,
                (null != attributeValue) ? Arrays.asList(attributeValue) : Arrays.asList(),
                null,
                false
        );

        // Attributes
        RequestAttributes requestAttr = new StdRequestAttributes(
                category,
                Arrays.asList((null!=attr)?new Attribute[]{attr}:new Attribute[]{}),
                null,
                null
        );

        return requestAttr;
    }
}
