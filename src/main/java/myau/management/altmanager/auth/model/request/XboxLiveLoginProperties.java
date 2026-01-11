package myau.management.altmanager.auth.model.request;

public class XboxLiveLoginProperties
{
    private final String AuthMethod;
    private final String SiteName;
    private final String RpsTicket;

    public XboxLiveLoginProperties(String AuthMethod, String SiteName, String RpsTicket)
    {
        this.AuthMethod = AuthMethod;
        this.SiteName = SiteName;
        this.RpsTicket = RpsTicket;
    }

    public String getAuthMethod()
    {
        return AuthMethod;
    }

    public String getSiteName()
    {
        return SiteName;
    }

    public String getRpsTicket()
    {
        return RpsTicket;
    }
}
