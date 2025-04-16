# Template

[`Templates`](https://github.com/amzn/app-platform/blob/main/presenter/public/src/commonMain/kotlin/software/amazon/app/platform/presenter/template/Template.kt)
are an abstraction between `Presenters` and `Renderers` and represent the root of the presenter and renderer tree.
Practically, a template is one particular type of `BaseModel` that hosts other models (a container of models).
However, instead of using a weak type like `List<BaseModel>`, a template carries semantics about what content should
be rendered, how many UI layers there are and where each individual model should be displayed.

`Templates` are app specific and not shared, because each app may use a different layering mechanism for individual
screen configurations. An example template definition could look like this:

```kotlin
sealed interface SampleAppTemplate : Template {

  data class FullScreenTemplate(
    val model: BaseModel,
  ) : SampleAppTemplate

  data class ListDetailTemplate(
    val list: BaseModel,
    val detail: BaseModel,
  ) : SampleAppTemplate
}
```
