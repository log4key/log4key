# Security Policy

## Overview

The security of Log4Key is important to us.

Log4Key is a logging library designed for high-throughput Java systems and may be embedded in applications, services, or observability pipelines.  
As such, security issues may have downstream impact depending on how the library is used.

We appreciate responsible disclosure of any vulnerabilities.

---

## Supported Versions

We currently support only the latest released version of Log4Key.

If you are using an older version, we recommend upgrading before reporting issues.

---

## Reporting a Vulnerability

If you discover a potential security vulnerability, **please report it privately**.

📧 **Email:** security@log4key.com

Please include as much detail as possible:

- Description of the issue
- Steps to reproduce (proof-of-concept if available)
- Affected versions
- Potential impact
- Suggested fix (optional)

⚠️ **Do not report security vulnerabilities via public GitHub issues or discussions.**

---

## Response Process

After receiving a report, we aim to:

- Acknowledge the report within **3 business days**
- Investigate and validate the issue
- Assess impact and severity
- Provide a fix, mitigation, or workaround

We may request additional information during the investigation.

---

## Responsible Disclosure

We follow a responsible disclosure process:

- Please do not publicly disclose the vulnerability before a fix is available
- We will notify you once the issue is resolved
- With your permission, we may credit you in the security advisory

---

## Scope

This policy applies to the core functionality of Log4Key, including:

- Log routing logic
- Key-based file or sink resolution
- Log formatting and output handling
- Built-in or officially supported extensions

---

## Potential Sensitive Areas

Due to the nature of Log4Key, particular attention should be given to:

- **File path routing based on dynamic keys** (e.g., path traversal risks)
- **Log injection or manipulation**
- **Data leakage through logs**
- **Custom sink integrations (network, storage, or external systems)**

If your report relates to any of these areas, please highlight it.

---

## Out of Scope

The following are generally out of scope:

- Issues caused by incorrect user configuration
- Vulnerabilities in third-party systems or integrations
- Performance optimizations without security impact

---

## Contact

For all security-related inquiries:

📧 security@log4key.com

For general questions, please use GitHub issues or discussions instead.

---

Thank you for helping keep Log4Key and its users secure.