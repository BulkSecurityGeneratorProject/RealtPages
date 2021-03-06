package by.koltun.web.rest.to.rent;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * {@link ApartmentRent} contact
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Contact {

    @JsonProperty(value = "owner")
    private Boolean isOwner;

    public Boolean getOwner() {
        return isOwner;
    }

    public void setOwner(Boolean owner) {
        isOwner = owner;
    }

    @Override
    public String toString() {
        return "Contact{" +
            "isOwner=" + isOwner +
            '}';
    }
}
