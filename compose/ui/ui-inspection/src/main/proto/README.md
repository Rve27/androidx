# Compose Inspector

## Change Descriptions

### Detect parameter changes for each recomposition

#### Added:
- `include_parameter_changes` to `StateReadSettings.All` and `StateReadSettings.ById`.
- `parameter_changes` to `StateReadGroup`.

#### version: compose.ui:ui:1.12.0-alpha02

#### Backwards compatibility:
`include_parameter_changes` will be false since an older client doesn't know about it.
The inspector will not generate `parameter_changes` for the `StateReadGroup`.

#### Forwards compatibility:
An older inspector will ignore the `include_parameter_changes` setting.
A client expecting this functionality will simply get an
empty list of `parameter_changes` from an older inspector.
