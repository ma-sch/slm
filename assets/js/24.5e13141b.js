(window.webpackJsonp=window.webpackJsonp||[]).push([[24],{271:function(t,e,s){"use strict";s.r(e);var n=s(279),a=s.n(n),o={data:()=>({selected:void 0,options:[]}),created:async function(){try{let t=await a.a.get("https://api.github.com/repos/eclipse-slm/slm/git/trees/github-pages");const e=t.data.tree.find(t=>"version"===t.path.toLowerCase());t=await a.a.get(e.url),this.options=t.data.tree.map(t=>({value:t.path,text:t.path})),this.options.sort((t,e)=>{const s=t.text.split("."),n=e.text.split(".");for(let t=0;t<s.length&&t<n.length;t++){const e=parseInt(s[t]),a=parseInt(n[t]);if(e!==a)return a-e;if(s[t]!==n[t])return n[t]-s[t]}return t.text===e.text?0:e.text<t.text?-1:1}),this.options.unshift({value:"latest",text:"latest"});const s=window.location.pathname.toLowerCase();let n=new RegExp("/version/([0-9]+.[0-9]+)");if(n.test(s)){let t=n.exec(s)[1];this.selected=t}else this.selected="latest"}catch(t){}},methods:{onChange(t){let e="";e="latest"===this.selected?"":"/version/"+this.selected;let s=window.location.pathname.toLowerCase(),n=new RegExp("/version/[0-9]+.[0-9]+");if(n.test(s)){let t=n.exec(s)[0];s=s.replace(t,e)}else s=e+s;window.location.pathname=s}}},l=s(15),i=Object(l.a)(o,(function(){var t=this,e=t._self._c;return t.options&&t.options.length>0?e("span",{staticClass:"nav-item",attrs:{"data-app":""}},[e("v-row",[e("v-col",{attrs:{cols:"3",align:"right"}},[t._v("Version:")]),t._v(" "),e("v-col",{attrs:{cols:"5",align:"left"}},[e("v-select",{attrs:{items:t.options,outlined:"",dense:""},on:{change:t.onChange},model:{value:t.selected,callback:function(e){t.selected=e},expression:"selected"}})],1)],1)],1):t._e()}),[],!1,null,null,null);e.default=i.exports}}]);