package com.hibiup.xacml.test;

import com.att.research.xacml.api.Request;
import com.att.research.xacml.api.Response;
import com.att.research.xacml.api.pdp.*;
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

@RunWith(JUnit4.class)
public class LocalPolicesTest {
    private static final Log logger	= LogFactory.getLog(LocalPolicesTest.class);

    public LocalPolicesTest() throws IOException {
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

    @Test
    public synchronized void testLocalPdpEngine() throws FactoryException, DOMStructureException, PDPException {
        String testPolicy = "IIA004";

        policyRepository.getPolicy(testPolicy).setXACMLProperties();

        PDPEngineFactory pdpEngineFactory	= PDPEngineFactory.newInstance();
        //pdpEngineFactory.setScopeResolver(scopeResolver);

        PDPEngine pdpEngine		= pdpEngineFactory.newEngine();

        Request request = DOMRequest.load(new File(pdpRepositoryLocation + "/requests/" +testPolicy + "Request.xml"));
        Response response	= pdpEngine.decide(request);
        response.getResults().forEach(f -> {
            String decision = f.getDecision().name();
            logger.info(decision);
            if(testPolicy == "IIA001") assert(decision=="PERMIT");
            else if (testPolicy == "IIA002") assert(decision=="NOTAPPLICABLE");
        });
    }
}
