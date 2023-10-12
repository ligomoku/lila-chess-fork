package views.html.team

import controllers.routes
import play.api.data.{ Field, Form }
import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.team.{ Team, TeamSecurity }

object admin:

  import trans.team.*

  def leaders(t: Team.WithLeaders, form: Form[?])(using PageContext) =
    views.html.base.layout(
      title = s"${t.name} • ${teamLeaders.txt()}",
      moreCss = frag(cssTag("team"), cssTag("tagify")),
      moreJs = jsModule("team.admin")
    ):
      main(cls := "page-menu page-small")(
        bits.menu(none),
        div(cls := "page-menu__content box")(
          adminTop(t.team, teamLeaders),
          p(cls := "box__pad")(onlyInviteLeadersTrust()),
          postForm(cls := "team-leaders form3", action := routes.Team.leaders(t.id))(
            globalError(form).map(_(cls := "box__pad")),
            table(cls := "slist slist-pad")(
              thead:
                tr(
                  th,
                  TeamSecurity.Permission.values.map: perm =>
                    th(st.title := perm.desc)(perm.name)
                )
              ,
              tbody:
                t.leaders.mapWithIndex: (l, i) =>
                  tr(
                    td(
                      userIdLink(l.user.some),
                      form3.hidden(s"leaders[$i].name", l.user)
                    ),
                    TeamSecurity.Permission.values.map: perm =>
                      td:
                        st.input(
                          tpe   := "checkbox",
                          name  := s"leaders[$i].perms[]",
                          value := perm.key,
                          l.perms.contains(perm) option st.checked
                        )
                  )
            ),
            form3.actions(cls := "box__pad")(
              a(href := routes.Team.show(t.id))(trans.cancel()),
              form3.submit(trans.save())
            )
          )
        )
      )

  def kick(t: Team, form: Form[?])(using PageContext) =
    views.html.base.layout(
      title = s"${t.name} • ${kickSomeone.txt()}",
      moreCss = frag(cssTag("team"), cssTag("tagify")),
      moreJs = jsModule("team.admin")
    ) {
      main(cls := "page-menu page-small")(
        bits.menu(none),
        div(cls := "page-menu__content box box-pad")(
          adminTop(t, kickSomeone),
          postForm(action := routes.Team.kick(t.id))(
            form3.group(form("members"), frag(whoToKick()))(teamMembersAutoComplete(t)),
            form3.actions(
              a(href := routes.Team.show(t.id))(trans.cancel()),
              form3.submit(trans.save())
            )
          )
        )
      )
    }

  private def teamMembersAutoComplete(team: Team)(field: Field) =
    form3.textarea(field)(rows := 2, dataRel := team.id)

  def pmAll(
      t: Team,
      form: Form[?],
      tours: List[lila.tournament.Tournament],
      unsubs: Int,
      limiter: (Int, Instant)
  )(using ctx: PageContext) =
    views.html.base.layout(
      title = s"${t.name} • ${messageAllMembers.txt()}",
      moreCss = cssTag("team"),
      moreJs = embedJsUnsafeLoadThen("""
$('.copy-url-button').on('click', function(e) {
$('#form3-message').val($('#form3-message').val() + e.target.dataset.copyurl + '\n')
})""")
    ) {
      main(cls := "page-menu page-small")(
        bits.menu(none),
        div(cls := "page-menu__content box box-pad")(
          adminTop(t, messageAllMembers),
          p(messageAllMembersLongDescription()),
          tours.nonEmpty option div(cls := "tournaments")(
            p(youWayWantToLinkOneOfTheseTournaments()),
            p(
              ul(
                tours.map { t =>
                  li(
                    tournamentLink(t),
                    " ",
                    momentFromNow(t.startsAt),
                    " ",
                    a(
                      dataIcon     := licon.Forward,
                      cls          := "text copy-url-button",
                      data.copyurl := s"${netConfig.domain}${routes.Tournament.show(t.id).url}"
                    )
                  )
                }
              )
            ),
            br
          ),
          postForm(cls := "form3", action := routes.Team.pmAllSubmit(t.id))(
            form3.group(
              form("message"),
              trans.message(),
              help = frag(
                pluralizeLocalize("member", unsubs),
                " out of ",
                t.nbMembers.localize,
                " (",
                f"${(unsubs * 100d) / t.nbMembers}%1.1f",
                "%)",
                " have unsubscribed from messages."
              ).some
            )(form3.textarea(_)(rows := 10)),
            limiter match
              case (remaining, until) =>
                frag(
                  p(cls := (remaining <= 0).option("error"))(
                    "You can send up to ",
                    lila.app.mashup.TeamInfo.pmAllCredits,
                    " team messages per week. ",
                    strong(remaining),
                    " messages remaining until ",
                    momentFromNowOnce(until),
                    "."
                  ),
                  form3.actions(
                    a(href := routes.Team.show(t.slug))(trans.cancel()),
                    remaining > 0 option form3.submit(trans.send())
                  )
                )
          )
        )
      )
    }

  private def adminTop(t: Team, i18n: lila.i18n.I18nKey)(using Lang) =
    boxTop:
      h1(a(href := routes.Team.show(t.slug))(t.name), " • ", i18n())
