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
package uk.ac.ed.ph.qtiworks.services;

import uk.ac.ed.ph.qtiworks.QtiWorksLogicException;
import uk.ac.ed.ph.qtiworks.QtiWorksRuntimeException;
import uk.ac.ed.ph.qtiworks.base.services.Auditor;
import uk.ac.ed.ph.qtiworks.domain.DomainConstants;
import uk.ac.ed.ph.qtiworks.domain.DomainEntityNotFoundException;
import uk.ac.ed.ph.qtiworks.domain.IdentityContext;
import uk.ac.ed.ph.qtiworks.domain.Privilege;
import uk.ac.ed.ph.qtiworks.domain.PrivilegeException;
import uk.ac.ed.ph.qtiworks.domain.dao.AssessmentDao;
import uk.ac.ed.ph.qtiworks.domain.dao.AssessmentPackageDao;
import uk.ac.ed.ph.qtiworks.domain.entities.Assessment;
import uk.ac.ed.ph.qtiworks.domain.entities.AssessmentPackage;
import uk.ac.ed.ph.qtiworks.domain.entities.User;
import uk.ac.ed.ph.qtiworks.services.domain.AssessmentPackageFileImportException;
import uk.ac.ed.ph.qtiworks.services.domain.AssessmentStateException;
import uk.ac.ed.ph.qtiworks.services.domain.AssessmentStateException.APSFailureReason;

import uk.ac.ed.ph.jqtiplus.exception2.QtiLogicException;
import uk.ac.ed.ph.jqtiplus.internal.util.Assert;
import uk.ac.ed.ph.jqtiplus.internal.util.StringUtilities;
import uk.ac.ed.ph.jqtiplus.node.AssessmentObjectType;
import uk.ac.ed.ph.jqtiplus.node.item.AssessmentItem;
import uk.ac.ed.ph.jqtiplus.node.test.AssessmentTest;
import uk.ac.ed.ph.jqtiplus.reading.QtiXmlObjectReader;
import uk.ac.ed.ph.jqtiplus.reading.QtiXmlReader;
import uk.ac.ed.ph.jqtiplus.resolution.AssessmentObjectManager;
import uk.ac.ed.ph.jqtiplus.utils.contentpackaging.QtiContentPackageExtractor;
import uk.ac.ed.ph.jqtiplus.validation.AssessmentObjectValidationResult;
import uk.ac.ed.ph.jqtiplus.xmlutils.CustomUriScheme;
import uk.ac.ed.ph.jqtiplus.xmlutils.XmlReadResult;
import uk.ac.ed.ph.jqtiplus.xmlutils.XmlResourceNotFoundException;
import uk.ac.ed.ph.jqtiplus.xmlutils.locators.ChainedResourceLocator;
import uk.ac.ed.ph.jqtiplus.xmlutils.locators.FileSandboxResourceLocator;
import uk.ac.ed.ph.jqtiplus.xmlutils.locators.NetworkHttpResourceLocator;
import uk.ac.ed.ph.jqtiplus.xmlutils.locators.ResourceLocator;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;

/**
 * Services for managing {@link Assessment}s
 *
 * @author David McKain
 */
@Transactional(readOnly=false)
@Service
public class AssessmentManagementServices {

    private static final Logger logger = LoggerFactory.getLogger(AssessmentManagementServices.class);

    public static final String DEFAULT_IMPORT_TITLE = "My Assessment";

    @Resource
    private Auditor auditor;

    @Resource
    private IdentityContext identityContext;

    @Resource
    private FilespaceManager filespaceManager;

    @Resource
    private AssessmentPackageFileImporter assessmentPackageFileImporter;

    @Resource
    private AssessmentDao assessmentDao;

    @Resource
    private AssessmentPackageDao assessmentPackageDao;

    @Resource
    private QtiXmlReader qtiXmlReader;

    //-------------------------------------------------

    @Transactional(propagation=Propagation.REQUIRED)
    public @Nonnull Assessment getAssessment(@Nonnull final Long assessmentId)
            throws DomainEntityNotFoundException, PrivilegeException {
        Assert.ensureNotNull(assessmentId, "assessmentId");
        final Assessment result = assessmentDao.requireFindById(assessmentId);
        ensureCallerMayView(result);
        return result;
    }

    @Transactional(propagation=Propagation.REQUIRED)
    public @Nonnull AssessmentPackage getCurrentAssessmentPackage(@Nonnull final Assessment assessment) {
        Assert.ensureNotNull(assessment, "assessment");
        final AssessmentPackage result = assessmentPackageDao.getCurrentAssessmentPackage(assessment);
        if (result==null) {
            throw new QtiWorksLogicException("Expected to always find at least 1 AssessmentPackage associated with an Assessment. Check the JPA-QL query and the logic in this class");
        }
        return result;
    }

    @Transactional(propagation=Propagation.REQUIRED)
    public @Nonnull List<Assessment> getCallerAssessments() {
        return assessmentDao.getForOwner(identityContext.getCurrentThreadEffectiveIdentity());
    }

    /**
     * Creates and persists a new {@link Assessment} and initial {@link AssessmentPackage}
     * from the data provided by the given {@link InputStream} and having the given content type.
     * <p>
     * Success post-conditions:
     * - the {@link InputStream} is left open
     * - a new {@link AssessmentPackage} is persisted, and its data is safely stored in a sandbox
     *
     * @param inputStream
     * @param contentType
     * @param name for the resulting package. A default will be chosen if one is not provided.
     *   The name will be silently truncated if it is too large for the underlying DB field.
     *
     * @throws PrivilegeException if the caller is not allowed to perform this action
     * @throws AssessmentPackageFileImportException
     * @throws QtiWorksRuntimeException
     */
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public @Nonnull Assessment importAssessment(@Nonnull final InputStream inputStream,
            @Nonnull final String contentType, @Nullable final String name)
            throws PrivilegeException, AssessmentPackageFileImportException {
        Assert.ensureNotNull(inputStream, "inputStream");
        Assert.ensureNotNull(contentType, "contentType");
        final User caller = ensureCallerMayCreateAssessment();

        /* First, upload the data into a sandbox */
        final AssessmentPackage assessmentPackage = importPackageFiles(inputStream, contentType);

        /* Create resulting Assessment entity */
        final Assessment assessment = new Assessment();
        assessment.setAssessmentType(assessmentPackage.getAssessmentType());
        assessment.setOwner(caller);

        /* Decide on resulting Assessment name, using a suitable default if client failed to supply anything */
        String resultingName;
        if (StringUtilities.isNullOrBlank(name)) {
            resultingName = assessmentPackage.getAssessmentType()==AssessmentObjectType.ASSESSMENT_ITEM ? "Item" : "Test";
        }
        else {
            resultingName = ServiceUtilities.trimString(name, DomainConstants.ASSESSMENT_NAME_MAX_LENGTH);
        }
        assessment.setName(resultingName);

        /* Guess a title */
        final String guessedTitle = guessAssessmentTitle(assessmentPackage);
        final String resultingTitle = !StringUtilities.isNullOrEmpty(guessedTitle) ? guessedTitle : DEFAULT_IMPORT_TITLE;
        assessment.setTitle(ServiceUtilities.trimSentence(resultingTitle, DomainConstants.ASSESSMENT_TITLE_MAX_LENGTH));

        /* Relate Assessment & AssessmentPackage */
        assessmentPackage.setAssessment(assessment);
        assessmentPackage.setImportVersion(Long.valueOf(1L));
        assessment.setPackageImportVersion(Long.valueOf(1L));

        /* Persist entities */
        try {
            assessmentDao.persist(assessment);
            assessmentPackageDao.persist(assessmentPackage);
        }
        catch (final Exception e) {
            logger.warn("Persistence of AssessmentPackage failed - deleting its sandbox", assessmentPackage);
            deleteAssessmentPackageSandbox(assessmentPackage);
            throw new QtiWorksRuntimeException("Failed to persist AssessmentPackage " + assessmentPackage, e);
        }
        logger.debug("Created new Assessment #{} with package #{}", assessment.getId(), assessmentPackage.getId());
        auditor.recordEvent("Created Assessment #" + assessment.getId() + " and AssessmentPackage #" + assessmentPackage.getId());
        return assessment;
    }

    /**
     * NOTE: Not allowed to go item->test or test->item.
     *
     * @param inputStream
     * @param contentType
     * @throws AssessmentStateException
     * @throws PrivilegeException
     * @throws AssessmentPackageFileImportException
     * @throws DomainEntityNotFoundException
     */
    @Transactional(propagation=Propagation.REQUIRES_NEW)
    public @Nonnull Assessment updateAssessmentPackageFiles(@Nonnull final Long assessmentId,
            @Nonnull final InputStream inputStream, @Nonnull final String contentType)
            throws AssessmentStateException, PrivilegeException,
            AssessmentPackageFileImportException, DomainEntityNotFoundException {
        Assert.ensureNotNull(assessmentId, "assessmentId");
        Assert.ensureNotNull(inputStream, "inputStream");
        Assert.ensureNotNull(contentType, "contentType");
        final Assessment assessment = assessmentDao.requireFindById(assessmentId);
        ensureCallerMayChange(assessment);

        /* Upload data into a new sandbox */
        final AssessmentPackage newAssessmentPackage = importPackageFiles(inputStream, contentType);

        /* Make sure we haven't gone item->test or test->item */
        if (newAssessmentPackage.getAssessmentType()!=assessment.getAssessmentType()) {
            throw new AssessmentStateException(APSFailureReason.CANNOT_CHANGE_ASSESSMENT_TYPE,
                    assessment.getAssessmentType(), newAssessmentPackage.getAssessmentType());
        }

        /* Join together */
        final long newPackageVersion = assessment.getPackageImportVersion().longValue() + 1;
        newAssessmentPackage.setImportVersion(newPackageVersion);
        newAssessmentPackage.setAssessment(assessment);
        assessment.setPackageImportVersion(newPackageVersion);

        /* Finally update DB */
        try {
            assessmentDao.update(assessment);
            assessmentPackageDao.persist(newAssessmentPackage);
        }
        catch (final Exception e) {
            logger.warn("Failed to update state of AssessmentPackage {} after file replacement - deleting new sandbox", e);
            deleteAssessmentPackageSandbox(newAssessmentPackage);
            throw new QtiWorksRuntimeException("Failed to update AssessmentPackage entity " + assessment, e);
        }
        logger.debug("Updated Assessment #{} to have package #{}", assessment.getId(), newAssessmentPackage.getId());
        auditor.recordEvent("Updated Assessment #" + assessment.getId() + " with AssessmentPackage #" + newAssessmentPackage.getId());
        return assessment;
    }

    @Transactional(propagation=Propagation.REQUIRED)
    @SuppressWarnings("unused")
    public void deleteAssessment(@Nonnull final Assessment assessment)
            throws AssessmentStateException, PrivilegeException {
        /* In order to do this correctly, we need to delete all state that might have
         * been associated with this assessment as well, so we'll come back to this...
         */
        throw new QtiLogicException("Not yet implemented!");
    }

    /**
     * DEV NOTES:
     *
     * - Forbid deletion of the only remaining package, as that ensures there's always a most
     *   recent package
     */
    @Transactional(propagation=Propagation.REQUIRED)
    @SuppressWarnings("unused")
    public void deleteAssessmentPackage(@Nonnull final AssessmentPackage assessmentPackage)
            throws AssessmentStateException, PrivilegeException {
        /* In order to do this correctly, we need to delete all state that might have
         * been associated with this package as well, so we'll come back to this...
         */
        throw new QtiLogicException("Not yet implemented!");
    }

    //-------------------------------------------------
    // Validation

    @Transactional(propagation=Propagation.REQUIRED)
    public AssessmentObjectValidationResult<?> validateAssessment(@Nonnull final Long assessmentId)
            throws PrivilegeException, DomainEntityNotFoundException {
        final Assessment assessment = getAssessment(assessmentId);
        final AssessmentPackage currentAssessmentPackage = getCurrentAssessmentPackage(assessment);

        /* Run the validation process */
        final AssessmentObjectValidationResult<?> validationResult = validateAssessment(currentAssessmentPackage);

        /* Persist results */
        currentAssessmentPackage.setValidated(true);
        currentAssessmentPackage.setValid(validationResult.isValid());
        assessmentPackageDao.update(currentAssessmentPackage);

        return validationResult;

    }

    /**
     * (This is not private as it is also called by the anonymous upload/validate action featured
     * in the first iteration of QTI Works. TODO: Decide whether we'll keep this kind of
     * functionality.)
     *
     * @param assessmentPackage
     * @return
     */
    @SuppressWarnings("unchecked")
    <E extends AssessmentObjectValidationResult<?>> AssessmentObjectValidationResult<?>
    validateAssessment(@Nonnull final AssessmentPackage assessmentPackage) {
        final File importSandboxDirectory = new File(assessmentPackage.getSandboxPath());
        final String assessmentObjectHref = assessmentPackage.getAssessmentHref();
        final AssessmentObjectType assessmentObjectType = assessmentPackage.getAssessmentType();
        final CustomUriScheme packageUriScheme = QtiContentPackageExtractor.PACKAGE_URI_SCHEME;
        final ResourceLocator inputResourceLocator = filespaceManager.createSandboxInputResourceLocator(importSandboxDirectory);
        final QtiXmlObjectReader objectReader = qtiXmlReader.createQtiXmlObjectReader(inputResourceLocator);
        final AssessmentObjectManager objectManager = new AssessmentObjectManager(objectReader);
        final URI objectSystemId = packageUriScheme.pathToUri(assessmentObjectHref);
        E result;
        if (assessmentObjectType==AssessmentObjectType.ASSESSMENT_ITEM) {
            result = (E) objectManager.resolveAndValidateItem(objectSystemId);
        }
        else if (assessmentObjectType==AssessmentObjectType.ASSESSMENT_TEST) {
            result = (E) objectManager.resolveAndValidateTest(objectSystemId);
        }
        else {
            throw new QtiWorksLogicException("Unexpected branch " + assessmentObjectType);
        }
        return result;
    }


    //-------------------------------------------------

    public ResourceLocator createAssessmentResourceLocator(final AssessmentPackage assessmentPackage) {
        final File sandboxDirectory = new File(assessmentPackage.getSandboxPath());
        final CustomUriScheme packageUriScheme = QtiContentPackageExtractor.PACKAGE_URI_SCHEME;
        final ChainedResourceLocator result = new ChainedResourceLocator(
                new FileSandboxResourceLocator(packageUriScheme, sandboxDirectory), /* (to resolve things in this package) */
                QtiXmlReader.JQTIPLUS_PARSER_RESOURCE_LOCATOR, /* (to resolve internal HTTP resources, e.g. RP templates) */
                new NetworkHttpResourceLocator() /* (to resolve external HTTP resources, e.g. RP templates, external items) */
        );
        return result;
    }

    public URI createAssessmentObjectUri(final AssessmentPackage assessmentPackage) {
        final CustomUriScheme packageUriScheme = QtiContentPackageExtractor.PACKAGE_URI_SCHEME;
        return packageUriScheme.pathToUri(assessmentPackage.getAssessmentHref().toString());
    }

    //-------------------------------------------------

    /**
     * @throws QtiWorksLogicException if sandboxPath is already null
     */
    private void deleteAssessmentPackageSandbox(final AssessmentPackage assessmentPackage) {
        final String sandboxPath = assessmentPackage.getSandboxPath();
        if (sandboxPath==null) {
            throw new QtiWorksLogicException("AssessmentPackage sandbox is null");
        }
        filespaceManager.deleteSandbox(new File(sandboxPath));
        assessmentPackage.setSandboxPath(null);
    }

    /**
     * @throws PrivilegeException
     * @throws AssessmentPackageFileImportException
     * @throws QtiWorksRuntimeException
     */
    private AssessmentPackage importPackageFiles(final InputStream inputStream, final String contentType)
            throws PrivilegeException, AssessmentPackageFileImportException {
        final User owner = identityContext.getCurrentThreadEffectiveIdentity();
        final File packageSandbox = filespaceManager.createAssessmentPackageSandbox(owner);
        try {
            final AssessmentPackage assessmentPackage = assessmentPackageFileImporter.importAssessmentPackageData(packageSandbox, inputStream, contentType);
            assessmentPackage.setImporter(owner);
            return assessmentPackage;
        }
        catch (final AssessmentPackageFileImportException e) {
            filespaceManager.deleteSandbox(packageSandbox);
            throw e;
        }
    }

    /**
     * Attempts to extract the title from an {@link AssessmentItem} or {@link AssessmentTest} for
     * bootstrapping the initial state of the resulting {@link AssessmentPackage}.
     * <p>
     * This performs a low level XML parse to save time; proper read/validation using JQTI+
     * is expected to happen later on.
     *
     * @param assessmentPackage
     * @return guessed title, or an empty String if nothing could be guessed.
     */
    private String guessAssessmentTitle(final AssessmentPackage assessmentPackage) {
        final File importSandboxDirectory = new File(assessmentPackage.getSandboxPath());
        final CustomUriScheme packageUriScheme = QtiContentPackageExtractor.PACKAGE_URI_SCHEME;
        final URI assessmentSystemId = packageUriScheme.pathToUri(assessmentPackage.getAssessmentHref());
        final ResourceLocator inputResourceLocator = filespaceManager.createSandboxInputResourceLocator(importSandboxDirectory);
        XmlReadResult xmlReadResult;
        try {
            xmlReadResult = qtiXmlReader.read(assessmentSystemId, inputResourceLocator, false);
        }
        catch (final XmlResourceNotFoundException e) {
            throw new QtiWorksLogicException("Assessment resource not found, which import process should have guarded against", e);
        }
        /* Let's simply extract the title attribute from the document element, and not worry about
         * anything else at this point.
         */
        final Document document = xmlReadResult.getDocument();
        return document!=null ? document.getDocumentElement().getAttribute("title") : "";
    }

    //-------------------------------------------------

    /**
     * TODO: Currently only permitting people seeing their own Assessments
     */
    private User ensureCallerMayView(final Assessment assessment)
            throws PrivilegeException {
        final User caller = identityContext.getCurrentThreadEffectiveIdentity();
        if (!assessment.getOwner().equals(caller)) {
            throw new PrivilegeException(caller, Privilege.VIEW_ASSESSMENT);
        }
        return caller;
    }

    private User ensureCallerMayChange(final Assessment assessment)
            throws PrivilegeException {
        final User caller = identityContext.getCurrentThreadEffectiveIdentity();
        if (!assessment.getOwner().equals(caller)) {
            throw new PrivilegeException(caller, Privilege.CHANGE_ASSESSMENT);
        }
        return caller;
    }

    /**
     * TODO: Currently we are only allowing instructors to create Assessments.
     * We may choose to let anonymous users do the same in future.
     */
    private User ensureCallerMayCreateAssessment() throws PrivilegeException {
        final User caller = identityContext.getCurrentThreadEffectiveIdentity();
        if (!caller.isInstructor()) {
            throw new PrivilegeException(caller, Privilege.CREATE_ASSESSMENT);
        }
        return caller;
    }

}
