/* Copyright (c) 2012, University of Edinburgh.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 * * Neither the name of the University of Edinburgh nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * This software is derived from (and contains code from) QTItools and MathAssessEngine.
 * QTItools is (c) 2008, University of Southampton.
 * MathAssessEngine is (c) 2010, University of Edinburgh.
 */
package uk.ac.ed.ph.jqtiplus.test.integration;

import static org.junit.Assert.assertEquals;

import uk.ac.ed.ph.qtiworks.samples.QtiSampleResource;
import uk.ac.ed.ph.qtiworks.samples.QtiSampleResource.Feature;
import uk.ac.ed.ph.qtiworks.samples.StandardQtiSampleSet;

import uk.ac.ed.ph.jqtiplus.reading.QtiXmlObjectReader;
import uk.ac.ed.ph.jqtiplus.reading.QtiXmlReader;
import uk.ac.ed.ph.jqtiplus.resolution.AssessmentObjectManager;
import uk.ac.ed.ph.jqtiplus.testutils.TestUtils;
import uk.ac.ed.ph.jqtiplus.validation.ItemValidationResult;
import uk.ac.ed.ph.jqtiplus.xmlutils.locators.ClassPathResourceLocator;
import uk.ac.ed.ph.jqtiplus.xmlutils.locators.ResourceLocator;

import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Integration test that runs our validation process on each IMS sample checking the validity
 * of each one that's supposed to be valid.
 *
 * @author David McKain
 */
@RunWith(Parameterized.class)
public class ValidationSampleTests {
    
    private QtiSampleResource qtiSampleResource;
    
    @Parameters
    public static Collection<Object[]> data() {
        return TestUtils.makeTestParameters(StandardQtiSampleSet.instance());
    }
    
    public ValidationSampleTests(QtiSampleResource qtiSampleResource) {
        this.qtiSampleResource = qtiSampleResource;
        
    }
    
    @Test
    public void test() throws Exception {
        final ResourceLocator sampleResourceLocator = new ClassPathResourceLocator();
        
        final QtiXmlReader qtiXmlReader = new QtiXmlReader();
        final QtiXmlObjectReader objectReader = qtiXmlReader.createQtiXmlObjectReader(sampleResourceLocator);
        final AssessmentObjectManager objectManager = new AssessmentObjectManager(objectReader);
        ItemValidationResult result = objectManager.resolveAndValidateItem(qtiSampleResource.toClassPathUri());
        
        assertEquals(!qtiSampleResource.hasFeature(Feature.NOT_FULLY_VALID), result.isValid());
    }
}