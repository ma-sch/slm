package org.eclipse.slm.resource_management.service.rest.update;

public class UpdateInformation {

    private String version;

    private String date;

    private String installationUri;

    private String installationChecksum;

    public UpdateInformation(String version, String date, String installationUri, String installationChecksum) {
        this.version = version;
        this.date = date;
        this.installationUri = installationUri;
        this.installationChecksum = installationChecksum;
    }

    private UpdateInformation(Builder builder) {
        this.version = builder.version;
        this.date = builder.date;
        this.installationUri = builder.installationUri;
        this.installationChecksum = builder.installationChecksum;
    }

    public String getVersion() {
        return version;
    }

    public String getDate() {
        return date;
    }

    public String getInstallationUri() {
        return installationUri;
    }

    public String getInstallationChecksum() {
        return installationChecksum;
    }

    public static class Builder {
        private String version;
        private String date;
        private String installationUri;
        private String installationChecksum;

        public Builder setVersion(String version) {
            this.version = version;
            return this;
        }

        public Builder setDate(String date) {
            this.date = date;
            return this;
        }

        public Builder setInstallationUri(String installationUri) {
            this.installationUri = installationUri;
            return this;
        }

        public Builder setInstallationChecksum(String installationChecksum) {
            this.installationChecksum = installationChecksum;
            return this;
        }

        public UpdateInformation build() {
            return new UpdateInformation(this);
        }
    }
}
