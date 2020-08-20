import { h } from 'snabbdom'
import SimulCtrl from '../ctrl';
import { Pairing } from '../interfaces';
import { onInsert } from './util';
import { opposite } from 'chessground/util';

export default function(ctrl: SimulCtrl) {
  return h('div.game-list.now-playing.box__pad', ctrl.data.pairings.map(miniPairing(ctrl)));
};

const miniPairing = (ctrl: SimulCtrl) => (pairing: Pairing) => {
  const game = pairing.game,
    player = pairing.player;
  return h(`span.mini-game.mini-game-${game.id}.mini-game--init.is2d`, {
    class: {
      host: ctrl.data.host.gameId === game.id
    },
    attrs: {
      'data-state': `${game.fen},${game.orient},${game.lastMove}`,
      'data-live': game.clock ? game.id : ''
    },
    hook: onInsert(window.lichess.powertip.manualUserIn)
  }, [
    h('span.mini-game__player', [
      h('a.mini-game__user.ulpt', {
        href: `/@/${player.name}`
      }, [
        h('span.name', player.title ? [h('span.title', player.title), ' ', player.name] : [player.name]),
        ' ',
        h('span.rating', player.rating)
      ]),
      game.clock ?
        h(`span.mini-game__clock.mini-game__clock--${opposite(game.orient)}`, {
          'data-time': game.clock[opposite(game.orient)]
        }) :
        h('span.mini-game__result', game.winner ? (game.winner == game.orient ? 0 : 1) : '½'),
    ]),
    h('a.cg-wrap', {
      href: `/${game.id}/${game.orient}`
    }),
    h('span.mini-game__player', [
      h('span'),
      game.clock ?
        h(`span.mini-game__clock.mini-game__clock--${game.orient}`, {
          'data-time': game.clock[game.orient]
        }) :
        h('span.mini-game__result', game.winner ? (game.winner == game.orient ? 1 : 0) : '½'),
    ]),
  ])
};
