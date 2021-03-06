/* Copyright (c) 2012-2013, University of Edinburgh.
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
package uk.ac.ed.ph.qtiworks.domain.entities;

import uk.ac.ed.ph.qtiworks.QtiWorksRuntimeException;
import uk.ac.ed.ph.qtiworks.domain.DomainConstants;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * Represents a {@link User} who logs in directly into QTIWorks via its own login
 * mechanism.
 *
 * @author David McKain
 */
@Entity
@Table(name="system_users")
@NamedQueries({
    /* Looks up the User having the given loginName */
    @NamedQuery(name="SystemUser.findByLoginName",
            query="SELECT u"
                + "  FROM SystemUser u"
                +"   WHERE u.loginName = :loginName")
})
public class SystemUser extends User implements BaseEntity, Comparable<SystemUser> {

    private static final long serialVersionUID = 7821803746245696405L;

    @Basic(optional=false)
    @Column(name="login_name", length=DomainConstants.USER_LOGIN_NAME_MAX_LENGTH, updatable=false, unique=true)
    private String loginName;

    @Basic(optional=false)
    @Column(name="sysadmin", updatable=true)
    private boolean sysAdmin;


    @Basic(optional=false)
    @Column(name="password_salt", length=DomainConstants.USER_PASSWORD_SALT_LENGTH)
    private String passwordSalt;

    @Basic(optional=false)
    @Column(name="password_digest", length=DomainConstants.SHA1_DIGEST_LENGTH)
    private String passwordDigest;

    //------------------------------------------------------------

    public SystemUser() {
        super(UserType.SYSTEM, null);
    }

    public SystemUser(final UserRole userRole) {
        super(UserType.SYSTEM, userRole);
    }

    //------------------------------------------------------------

    @Override
    public String getBusinessKey() {
        ensureLoginName(this);
        return "system/" + loginName;
    }

    //------------------------------------------------------------

    public String getLoginName() {
        return loginName;
    }

    public void setLoginName(final String loginName) {
        this.loginName = loginName;
    }


    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(final String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }


    public String getPasswordDigest() {
        return passwordDigest;
    }

    public void setPasswordDigest(final String passwordDigest) {
        this.passwordDigest = passwordDigest;
    }


    @Override
    public boolean isSysAdmin() {
        return sysAdmin;
    }

    public void setSysAdmin(final boolean sysAdmin) {
        this.sysAdmin = sysAdmin;
    }

    //------------------------------------------------------------

    @Override
    public final int compareTo(final SystemUser o) {
        ensureLoginName(this);
        ensureLoginName(o);
        return loginName.compareTo(o.loginName);
    }

    private void ensureLoginName(final SystemUser user) {
        if (user.loginName==null) {
            throw new QtiWorksRuntimeException("Current logic branch requires loginName to be non-null on " + user);
        }
    }
}
