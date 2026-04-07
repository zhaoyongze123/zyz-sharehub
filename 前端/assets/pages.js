(function () {
  function byId(id) {
    return document.getElementById(id);
  }

  function setStatus(groupId, status) {
    var group = byId(groupId);
    if (!group) return;
    var items = group.querySelectorAll('[data-status]');
    items.forEach(function (item) {
      item.classList.toggle('is-active', item.getAttribute('data-status') === status);
    });

    var controls = document.querySelectorAll('[data-status-target="' + groupId + '"]');
    controls.forEach(function (btn) {
      btn.classList.toggle('is-active', btn.getAttribute('data-status-value') === status);
    });
  }

  function initStatus() {
    var groups = document.querySelectorAll('[data-status-group]');
    groups.forEach(function (group) {
      var defaultStatus = group.getAttribute('data-status-default') || 'loading';
      setStatus(group.id, defaultStatus);
    });

    document.querySelectorAll('[data-status-target]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        var target = btn.getAttribute('data-status-target');
        var value = btn.getAttribute('data-status-value');
        setStatus(target, value);
      });
    });
  }

  function setTab(groupName, tabKey) {
    var triggerSelector = '[data-tab-trigger][data-tab-group="' + groupName + '"]';
    document.querySelectorAll(triggerSelector).forEach(function (trigger) {
      trigger.classList.toggle('is-active', trigger.getAttribute('data-tab-key') === tabKey);
    });

    var panelSelector = '[data-tab-panel][data-tab-group="' + groupName + '"]';
    document.querySelectorAll(panelSelector).forEach(function (panel) {
      panel.classList.toggle('is-active', panel.getAttribute('data-tab-key') === tabKey);
    });
  }

  function initTabs() {
    var groups = {};
    document.querySelectorAll('[data-tab-trigger]').forEach(function (trigger) {
      var groupName = trigger.getAttribute('data-tab-group');
      if (!groups[groupName]) groups[groupName] = [];
      groups[groupName].push(trigger);
      trigger.addEventListener('click', function () {
        setTab(groupName, trigger.getAttribute('data-tab-key'));
      });
    });

    Object.keys(groups).forEach(function (groupName) {
      var active = groups[groupName].find(function (item) {
        return item.classList.contains('is-active');
      });
      var first = groups[groupName][0];
      var selected = active || first;
      if (selected) setTab(groupName, selected.getAttribute('data-tab-key'));
    });
  }

  function applyFilter(input) {
    var targetId = input.getAttribute('data-filter-target');
    var target = byId(targetId);
    if (!target) return;

    var query = input.value.trim().toLowerCase();
    var items = target.querySelectorAll('[data-filter-item]');
    var visible = 0;
    items.forEach(function (item) {
      var key = (item.getAttribute('data-filter-key') || item.textContent || '').toLowerCase();
      var match = !query || key.indexOf(query) > -1;
      item.hidden = !match;
      if (match) visible += 1;
    });

    var empty = document.querySelector('[data-filter-empty="' + targetId + '"]');
    if (empty) empty.hidden = visible !== 0;

    var count = document.querySelector('[data-filter-count="' + targetId + '"]');
    if (count) count.textContent = String(visible);
  }

  function initFilter() {
    document.querySelectorAll('[data-filter-input]').forEach(function (input) {
      input.addEventListener('input', function () {
        applyFilter(input);
      });
      applyFilter(input);
    });
  }

  function initCollapse() {
    document.querySelectorAll('[data-collapse-toggle]').forEach(function (btn) {
      var targetId = btn.getAttribute('data-collapse-toggle');
      var panel = byId(targetId);
      if (!panel) return;

      function sync(isOpen) {
        btn.setAttribute('aria-expanded', isOpen ? 'true' : 'false');
        panel.hidden = !isOpen;
      }

      var openByDefault = btn.getAttribute('data-collapse-open') === 'true';
      sync(openByDefault);
      btn.addEventListener('click', function () {
        var isOpen = btn.getAttribute('aria-expanded') !== 'true';
        sync(isOpen);
      });
    });
  }

  var toastTimer = null;
  function getToastNode() {
    var node = byId('app-toast');
    if (node) return node;
    node = document.createElement('div');
    node.id = 'app-toast';
    node.className = 'app-toast';
    node.setAttribute('role', 'status');
    node.setAttribute('aria-live', 'polite');
    document.body.appendChild(node);
    return node;
  }

  function showToast(message, type) {
    var node = getToastNode();
    node.textContent = message || '操作成功';
    node.classList.toggle('is-error', type === 'error');
    node.classList.add('is-show');

    if (toastTimer) window.clearTimeout(toastTimer);
    toastTimer = window.setTimeout(function () {
      node.classList.remove('is-show');
    }, 1800);
  }

  function initToast() {
    document.querySelectorAll('[data-toast]').forEach(function (btn) {
      btn.addEventListener('click', function () {
        showToast(btn.getAttribute('data-toast'), btn.getAttribute('data-toast-type') || 'default');
      });
    });

    window.AppPages = {
      setStatus: setStatus,
      setTab: setTab,
      showToast: showToast
    };
  }

  document.addEventListener('DOMContentLoaded', function () {
    initStatus();
    initTabs();
    initFilter();
    initCollapse();
    initToast();
  });
})();
