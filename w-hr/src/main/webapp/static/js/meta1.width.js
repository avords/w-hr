var _winWt = $(window).width(), _scrWt = window.screen.width;
var _wsv = (_scrWt / _winWt) - 0.05, _wsv = _wsv.toString(), _wsv = _wsv.substr(0,4);
document.write('<meta name="viewport" content="width='+ _scrWt +', initial-scale='+ _wsv +', user-scalable=1" />');
document.write('<meta name="format-detection" content="telephone=no, email=no, address=no" />');