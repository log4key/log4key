# Contributing to Log4Key

We welcome contributions! To ensure clarity, legal safety, and a smooth review process, please read this guide before submitting a pull request.

## Code of Conduct

All contributors are expected to follow our [Code of Conduct](CODE_OF_CONDUCT.md).

## How to Contribute

1. Fork the repository  
2. Create a feature branch (`git checkout -b feat/your-feature`)  
3. Commit your changes  
4. Push to your fork (`git push origin feat/your-feature`)  
5. Open a Pull Request  

### About `git commit -s` (Signed-off-by)

We recommend using the `-s` flag with `git commit`. For example:  
`git commit -s -m "Add useful feature"`

This adds a `Signed-off-by` line to your commit message, 
like: `Signed-off-by: Your Name <your.email@example.com>`

**What does it mean?**  
The `Signed-off-by` line certifies that you have the right to submit this contribution under the project's license terms. It follows the **Developer Certificate of Origin (DCO)**, a lightweight way to confirm contributor rights.

> 🔗 Learn more: [https://developercertificate.org/](https://developercertificate.org/)  
> 💡 Note: The `Signed-off-by` (DCO) is for audit and history purposes. The legally binding authorization comes from the signed CLA.  
> 💡 Tip: Always sign your commits (`git commit -s`) even if your CLA is signed. It ensures a clear audit trail for your contribution.

## Contributor License Agreement (CLA)

### Why do we require a CLA?

Log4Key is offered under a **dual-licensing model**:  
- The **community edition** is licensed under **GNU GPLv3**.  
- A **commercial edition**, which may include enhanced features or integrations, is available under a **proprietary license**.

To legally include your contributions in **both editions**, we need explicit authorization beyond the open source license.

### Scope of Your Contribution

By signing the CLA and submitting a contribution, you agree that your code can be:  
- Distributed under the open source license (**GNU GPLv3**) for the community edition.  
- Included in the proprietary commercial edition under the terms of the signed CLA.  

> ⚠️ Note: Your contributions may appear both in the **community edition (GPLv3)** and the **commercial edition (proprietary)**. Signing the CLA ensures legal use in both.

You retain full copyright and may use your code elsewhere independently.

### Which CLA do I need?

- **Individual contributors**: Please sign the [Individual CLA (ICLA.md)](ICLA.md).  
- **Contributing on behalf of a company**: Your employer must sign the [Corporate CLA (CCLA.md)](CCLA.md). You’ll also need to be listed as an authorized contributor.

### Contributing on Behalf of a Company

If you are contributing as an employee:  
- Your employer must have signed the Corporate CLA (CCLA).  
- You must be listed as an authorized contributor by your company.  
- Only submit contributions for which you have proper internal approval.

## Licensing and Code Submission

### Existing Code & License Compliance

- When submitting code that originates outside of Log4Key, you must have the right to license it under the Project’s CLA terms.  
- Avoid submitting code from other GPL, AGPL, or proprietary projects unless you are the copyright holder or have explicit permission.  
- Contributions should not include confidential or proprietary information from your employer or third parties.

### Licensing of Contributions

Unless explicitly stated otherwise, any contribution intentionally submitted for inclusion in the Project shall be licensed under the Project's open source license (**GNU GPLv3**) and may also be used in commercial editions as permitted by the signed CLA.

Your contributions are governed by the CLA, which explicitly authorizes:  
- Distribution under GPLv3 (for the open source community)  
- Use in proprietary (closed-source) software (for commercial offerings)

You retain full copyright to your code and may use it freely outside this project.

### Contribution Review

- The maintainers may review contributions for compliance with code style, security, and license compatibility.  
- Contributions are considered perpetual and irrevocable unless explicitly stated otherwise in writing.

## Code Style & Testing

- Follow existing code conventions  
- Add unit tests for new functionality  
- Ensure all tests pass before submitting  

Thank you for helping make Log4Key better!