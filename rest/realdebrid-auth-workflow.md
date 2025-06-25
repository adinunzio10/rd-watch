> **Real-Debrid Device Authentication Workflow**  
> You can use this client ID on opensource apps if you don't need custom scopes or name:  
> **Client ID:** `X245A4XAIBGVM`  
> Allowed scopes: `unrestrict`, `torrents`, `downloads`, `user`  
>  
> This client ID can have stricter limits than service limits due to poorly designed apps using it.

---

### Workflow for Opensource Apps

This authentication process is similar to OAuth2 for mobile devices, but opensource apps/scripts **cannot be shipped with a client_secret** (since it's meant to remain secret).

The goal is to get a new set of `client_id` and `client_secret` bound to the user. You may reuse these credentials later using OAuth2 for mobile devices.

> **Warning:**  
> Do not redistribute the credentials.  
> Usage with another account will display the UID of the user who obtained the credentials.  
> For example, instead of displaying "The most fabulous app" it will display "The most fabulous app (UID: 000)".

---

### Endpoints

- **Device endpoint:** `/device/code`
- **Credentials endpoint:** `/device/credentials`
- **Token endpoint:** `/token`

---

### Step-by-Step Flow

#### **Step 1 – Obtain Authentication Data**

Your application makes a direct request to the device endpoint, with the query string parameters `client_id` and `new_credentials=yes`, and obtains a JSON object with authentication data.

<details>
<summary>Example Request</summary>

```http
GET https://api.real-debrid.com/oauth/v2/device/code?client_id=X245A4XAIBGVM&new_credentials=yes
```
</details>

---

#### **Step 2 – User Verification**

Ask the user to go to the verification endpoint (provided by `verification_url`) and enter the code provided by `user_code`.

---

#### **Step 3 – Poll for Credentials**

Using the value of `device_code`, every 5 seconds your application starts making direct requests to the credentials endpoint, with the following query string parameters:

- `client_id`
- `code` (the value of `device_code`)

Your application will receive an error message until the user has entered the code and authorized the application.

---

#### **Step 4 – User Authorizes**

The user enters the code, logs in if necessary, and authorizes your application.

---

#### **Step 5 – Credentials Granted**

Once authorized, your application's call to the credentials endpoint returns a JSON object with:

- `client_id`: a new client_id bound to the user
- `client_secret`

Store these values for later requests.

---

#### **Step 6 – Obtain Access Token**

Using the value of `device_code`, your application makes a direct POST request to the token endpoint, with:

- `client_id` (from credentials endpoint)
- `client_secret` (from credentials endpoint)
- `code` (the value of `device_code`)
- `grant_type`: `http://oauth.net/grant_type/device/1.0`

The answer will be a JSON object with:

- `access_token`
- `expires_in` (token validity period, in seconds)
- `token_type` ("Bearer")
- `refresh_token` (expires only when your application rights are revoked by user)

<details>
<summary>Example cURL call</summary>

```http
POST https://api.real-debrid.com/oauth/v2/token
Content-Type: application/x-www-form-urlencoded

client_id=X245A4XAIBGVM
client_secret=Q7BB7JLY
code=DR36VK5VPT5GFTJKUNVMVIX3YUI4ADKFUQYMDW6W66MHX4TIWYGQ
grant_type=http://oauth.net/grant_type/device/1.0
```
</details>
