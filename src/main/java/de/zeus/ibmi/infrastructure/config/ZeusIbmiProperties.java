package de.zeus.ibmi.infrastructure.config;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zeus.ibmi")
public class ZeusIbmiProperties {

  @Valid private Db db = new Db();

  @Valid private Query query = new Query();

  @Valid private Output output = new Output();

  @Valid private Manifest manifest = new Manifest();

  public Db getDb() {
    return db;
  }

  public void setDb(Db db) {
    this.db = db;
  }

  public Query getQuery() {
    return query;
  }

  public void setQuery(Query query) {
    this.query = query;
  }

  public Output getOutput() {
    return output;
  }

  public void setOutput(Output output) {
    this.output = output;
  }

  public Manifest getManifest() {
    return manifest;
  }

  public void setManifest(Manifest manifest) {
    this.manifest = manifest;
  }

  public static class Db {
    private String driver;

    private String url;

    private String user;
    private String password;
    private String passwordEnv;
    private boolean allowEmptyPassword;

    public String getDriver() {
      return driver;
    }

    public void setDriver(String driver) {
      this.driver = driver;
    }

    public String getUrl() {
      return url;
    }

    public void setUrl(String url) {
      this.url = url;
    }

    public String getUser() {
      return user;
    }

    public void setUser(String user) {
      this.user = user;
    }

    public String getPassword() {
      return password;
    }

    public void setPassword(String password) {
      this.password = password;
    }

    public String getPasswordEnv() {
      return passwordEnv;
    }

    public void setPasswordEnv(String passwordEnv) {
      this.passwordEnv = passwordEnv;
    }

    public boolean isAllowEmptyPassword() {
      return allowEmptyPassword;
    }

    public void setAllowEmptyPassword(boolean allowEmptyPassword) {
      this.allowEmptyPassword = allowEmptyPassword;
    }
  }

  public static class Query {
    private String sql;
    private String file;

    private Integer fetchSize;

    private Integer timeoutSeconds;

    public String getSql() {
      return sql;
    }

    public void setSql(String sql) {
      this.sql = sql;
    }

    public String getFile() {
      return file;
    }

    public void setFile(String file) {
      this.file = file;
    }

    public Integer getFetchSize() {
      return fetchSize;
    }

    public void setFetchSize(Integer fetchSize) {
      this.fetchSize = fetchSize;
    }

    public Integer getTimeoutSeconds() {
      return timeoutSeconds;
    }

    public void setTimeoutSeconds(Integer timeoutSeconds) {
      this.timeoutSeconds = timeoutSeconds;
    }
  }

  public static class Output {
    private String directory;

    private List<String> formats = new ArrayList<>(List.of("xml", "json", "csv", "md"));

    public String getDirectory() {
      return directory;
    }

    public void setDirectory(String directory) {
      this.directory = directory;
    }

    public List<String> getFormats() {
      return formats;
    }

    public void setFormats(List<String> formats) {
      this.formats = formats;
    }
  }

  public static class Manifest {
    private boolean enabled = true;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }
}
