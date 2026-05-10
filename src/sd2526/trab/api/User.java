package sd2526.trab.api;

import java.util.Objects;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Represents a user in the system.
 */
@Entity
public class User {
	
	@Id
    private String name;
	
    private String pwd;
    private String domain;
    private String displayName;

    public User() {
        this.pwd = null;
        this.name = null;
        this.domain = null;
        this.displayName = null;
    }

    public User(String name, String pwd, String displayName, String domain) {
        this.pwd = pwd;
        this.name = name;
        this.domain = domain;
        this.displayName = displayName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPwd() {
        return pwd;
    }

    public void setPwd(String pwd) {
        this.pwd = pwd;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", pwd='" + pwd + '\'' +
                ", displayName='" + displayName + '\'' +
                ", domain='" + domain + '\'' +
                '}';
    }
    
    public User copyWithoutPassword() {
		return new User(name, pwd, displayName, domain);
	}
    
    public User updateFrom( User other ) {
		return new User( name, 
				other.pwd != null ? other.pwd : pwd, 
				other.displayName != null ? other.displayName : displayName,
				other.domain != null ? other.domain : domain);
	}
    
	public boolean matches(User other) {
		if (this == other)
			return true;
		if (other == null)
			return false;
		return Objects.equals(displayName, other.displayName) && Objects.equals(name, other.name) && Objects.equals(pwd, other.pwd);
	}
}
