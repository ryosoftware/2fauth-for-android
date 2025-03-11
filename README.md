<P ALIGN="CENTER"><IMG WIDTH="128" SRC="app/src/main/ic_launcher.png"></P>

# PURPOSE

The purpose of this app is to facilitate access to 2FA tokens managed through <A HREF="https://github.com/Bubka/2FAuth">Bubka's 2FAuth service</A>.

To do this, we have developed an Android App that reads 2FA accounts from a user server and stores them locally, allowing the generation of 2FA codes even when we are not connected to the server.

# MAIN FEATURES

Read the 2FA accounts for a user (identified by the server URL location and an access token) and generate OTP tokens offline.

Protection level: PIN password and fingerprint access.

# CERTIFICATE SIGNATURE VERIFICATION

The SHA-256 digest of the certificate used to sign the app is as follows, and remains constant regardless of the version:

`730d15ddea95e04a3d8201a577dfb7c5490dbf0f489f33de8061651067cd2582`

The app signature certification can be checked by the following command:

`apksigner verify --verbose --print-certs app-release.apk | grep "Signer #1 certificate SHA-256 digest"`

# OPEN SOURCE LIBRARIES WE USE

We generate the OTP codes using the JAVA library from <A HREF="https://github.com/BastiaanJansen/otp-java">Bastiaan Jansen</A>.

Because Bubka 2FA uses SVG icons by default and Android does not natively support that graphic format, we use <A HREF="https://github.com/homarr-labs/dashboard-icons">Dashboard Icons</A> to download icons that are in that format (to search for icons we use the 2FA account service name, in lowercase and with blank spaces replaced by dashes).

# DONATE

If you want, you can invite me to a coffee or a seafood platter

<A HREF="https://www.paypal.com/donate/?hosted_button_id=L46URT58CQNDJ"><IMG SRC="assets/paypal.png"></A>

# LICENSE

This app is licensed under the terms of the <A HREF="https://creativecommons.org/licenses/by-nc-sa/4.0/deed.en">CC BY-NC-SA 4.0</A> License.




